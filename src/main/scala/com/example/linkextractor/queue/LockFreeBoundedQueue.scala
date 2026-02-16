package com.example.linkextractor.queue

import java.util.concurrent.atomic.AtomicReference
import scala.annotation.tailrec
import scala.collection.immutable.{Queue => IQueue}
import scala.concurrent.{Future, Promise}

/**
 * Lock-free bounded queue backed by immutable Scala queues and CAS state
 * transitions.
 *
 * Key points:
 *   1. State is stored in one `AtomicReference[State]`.
 *   2. Producers and consumers modify state through compare-and-set (CAS)
 *      retries.
 *   3. Buffer operations are FIFO and amortized O(1).
 *   4. On overflow, the oldest buffered element is dropped before appending the
 *      new value.
 *   5. `size` is stored in state to keep capacity checks O(1) without calling
 *      `buffer.size` repeatedly during CAS retries.
 *
 * @param config
 *   Queue configuration.
 * @tparam A
 *   Element type.
 */
final class LockFreeBoundedQueue[A](config: QueueConfig.BoundedQueueConfig) extends Queue[A] {

  /**
   * Immutable queue state stored in the atomic reference.
   *
   * @param buffer
   *   Buffered FIFO data.
   * @param size
   *   Cached buffer size used for O(1) capacity checks.
   * @param waiters
   *   FIFO queue of pending dequeue promises.
   * @param closed
   *   Queue closed flag.
   */
  private final case class State(
    buffer: IQueue[A],
    size: Int,
    waiters: IQueue[Promise[Option[A]]],
    closed: Boolean
  ) {
    require(size >= 0, "size must be non-negative")
    require(size <= config.capacity, "size must not exceed capacity")
  }

  private val ref = new AtomicReference(
    State(
      buffer = IQueue.empty[A],
      size = 0,
      waiters = IQueue.empty[Promise[Option[A]]],
      closed = false
    )
  )

  /**
   * Enqueues one value using lock-free CAS transitions.
   *
   * Delivery priority:
   *   1. fail fast if closed,
   *   2. hand off directly to oldest waiter when present,
   *   3. otherwise append to bounded buffer with overflow policy.
   *
   * @param value
   *   Value to enqueue.
   * @return
   *   Enqueue result describing successful enqueue, overflow drop, or closed
   *   rejection.
   */
  override def enqueue(value: A): Queue.EnqueueResult = {

    /**
     * CAS retry loop for enqueue transitions.
     *
     * @return
     *   Final enqueue result once one transition is committed.
     */
    @tailrec def enqueueLoop(): Queue.EnqueueResult = {
      val current = ref.get()

      if (current.closed) Queue.EnqueueResult.Closed
      else {
        current.waiters.dequeueOption match {
          case Some((waiter, remainingWaiters)) =>
            val next = current.copy(waiters = remainingWaiters)
            if (ref.compareAndSet(current, next)) {
              waiter.trySuccess(Some(value))
              Queue.EnqueueResult.Enqueued
            } else {
              enqueueLoop()
            }

          case None =>
            val (nextBuffer, nextSize, result) = appendBounded(current.buffer, current.size, value)
            val next                           = current.copy(buffer = nextBuffer, size = nextSize)
            if (ref.compareAndSet(current, next)) result else enqueueLoop()
        }
      }
    }

    enqueueLoop()
  }

  /**
   * Dequeues one value, registering a waiter only when queue is empty and open.
   *
   * The method first attempts a lock-free buffered dequeue fast-path. If no
   * value is available, it either returns `None` for closed/drained state or
   * registers a promise waiter for future producer handoff.
   *
   * @return
   *   Future with one dequeued value, or `None` when queue is closed and
   *   drained.
   */
  override def dequeue(): Future[Option[A]] =
    claimBufferedValue() match {
      case some @ Some(_) => Future.successful(some)
      case None           => dequeueLoop(existingWaiter = None)
    }

  /**
   * Closes queue and resolves all pending waiters.
   *
   * Close is idempotent. Buffered values remain dequeueable after close.
   */
  override def close(): Unit = {

    /**
     * CAS retry loop for close transition.
     */
    @tailrec
    def closeLoop(): Unit = {
      val current = ref.get()
      if (current.closed) ()
      else {
        val next = current.copy(closed = true)
        if (ref.compareAndSet(current, next)) resolveWaitersAfterClose() else closeLoop()
      }
    }

    closeLoop()
  }

  /**
   * Debug view of queue state at a single instant.
   *
   * @return
   *   String representation of current state snapshot.
   */
  override def toString: String = {
    val state     = ref.get()
    val bufferStr = state.buffer.mkString("[", ", ", "]")
    s"LockFreeBoundedQueue(buffer=$bufferStr, size=${state.size}, waiters=${state.waiters.size}, closed=${state.closed})"
  }

  /**
   * CAS retry loop for dequeue waiter registration path.
   *
   * A single waiter promise is reused across retries to prevent duplicate
   * waiter entries.
   *
   * @param existingWaiter
   *   Existing waiter promise reused across CAS retries.
   * @return
   *   Future completed with one value, or `None` for closed/drained queue.
   */
  @tailrec
  private def dequeueLoop(existingWaiter: Option[Promise[Option[A]]]): Future[Option[A]] =
    claimBufferedValue() match {
      case value @ Some(_) => Future.successful(value)
      case None            =>
        val current = ref.get()
        if (current.closed) Future.successful(None)
        else {
          val waiter = existingWaiter.getOrElse(Promise[Option[A]]())
          val next   = current.copy(waiters = current.waiters.enqueue(waiter))
          if (ref.compareAndSet(current, next)) waiter.future else dequeueLoop(Some(waiter))
        }
    }

  /**
   * Resolves waiters after close by repeatedly:
   *   1. claiming one waiter from state,
   *   2. claiming one buffered value using shared dequeue fast-path logic,
   *   3. completing waiter with value or `None`.
   *
   * @return
   *   Unit when no waiters remain.
   */
  @tailrec
  private def resolveWaitersAfterClose(): Unit =
    pollWaiterAfterClose() match {
      case Some(waiter) =>
        waiter.trySuccess(claimBufferedValue())
        resolveWaitersAfterClose()
      case None => ()
    }

  /**
   * Removes one waiter from shared state after close.
   *
   * @return
   *   Oldest waiter if present.
   */
  @tailrec
  private def pollWaiterAfterClose(): Option[Promise[Option[A]]] = {
    val current = ref.get()
    current.waiters.dequeueOption match {
      case Some((waiter, remainingWaiters)) =>
        val next = current.copy(waiters = remainingWaiters)
        if (ref.compareAndSet(current, next)) Some(waiter) else pollWaiterAfterClose()
      case None => None
    }
  }

  /**
   * Attempts to claim one buffered value via CAS.
   *
   * This method is shared by normal `dequeue` fast-path and close-time waiter
   * resolution to avoid duplicated dequeue state logic.
   *
   * @return
   *   Claimed value when available.
   */
  @tailrec
  private def claimBufferedValue(): Option[A] = {
    val current = ref.get()
    current.buffer.dequeueOption match {
      case Some((value, tail)) =>
        val next = current.copy(buffer = tail, size = current.size - 1)
        if (ref.compareAndSet(current, next)) Some(value) else claimBufferedValue()
      case None => None
    }
  }

  /**
   * Appends one value with bounded overflow policy.
   *
   * Policy:
   *   1. If space exists, append and grow size.
   *   2. If full, drop oldest then append, keeping size unchanged.
   *
   * @param buffer
   *   Current buffered data.
   * @param size
   *   Current buffered size.
   * @param value
   *   Value to append.
   * @return
   *   Tuple of next buffer, next size, and enqueue result.
   */
  private def appendBounded(
    buffer: IQueue[A],
    size: Int,
    value: A
  ): (IQueue[A], Int, Queue.EnqueueResult) =
    if (size < config.capacity) (buffer.enqueue(value), size + 1, Queue.EnqueueResult.Enqueued)
    else {
      val (_, tail) = buffer.dequeue
      (
        tail.enqueue(value),
        size,
        Queue.EnqueueResult.EnqueuedAfterDroppingOldest
      )
    }
}
