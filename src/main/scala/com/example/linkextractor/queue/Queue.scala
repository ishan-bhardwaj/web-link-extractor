package com.example.linkextractor.queue

import scala.concurrent.Future

/**
 * Queue abstraction for producer-consumer workflows.
 *
 * @tparam A
 *   Element type.
 */
abstract class Queue[A] {

  /**
   * Enqueues one element.
   *
   * @param value
   *   Element to enqueue.
   * @return
   *   Enqueue outcome, including overflow/closed state semantics.
   */
  def enqueue(value: A): Queue.EnqueueResult

  /**
   * Dequeues one element asynchronously.
   *
   * @return
   *   `Some(value)` when data is available, or `None` when the queue is closed
   *   and drained.
   */
  def dequeue(): Future[Option[A]]

  /**
   * Closes the queue.
   *
   * Close should be idempotent. Buffered data remains available to consumers.
   * Pending waiters are completed from buffered data when possible and then
   * with `None`.
   */
  def close(): Unit
}

/**
 * Queue factories and queue domain types.
 */
object Queue {

  /**
   * Result type for enqueue operations.
   */
  sealed trait EnqueueResult

  object EnqueueResult {

    /**
     * Value was enqueued without overflow drop.
     */
    case object Enqueued extends EnqueueResult

    /**
     * Value was enqueued after dropping the oldest buffered element.
     */
    case object EnqueuedAfterDroppingOldest extends EnqueueResult

    /**
     * Queue was closed and value was rejected.
     */
    case object Closed extends EnqueueResult
  }

  /**
   * Creates a lock-free bounded queue from explicit configuration.
   *
   * @param config
   *   Bounded queue configuration.
   * @tparam A
   *   Element type.
   * @return
   *   Bounded lock-free queue instance.
   */
  def bounded[A](config: QueueConfig.BoundedQueueConfig): Queue[A] =
    new LockFreeBoundedQueue[A](config)

  /**
   * Creates a lock-free bounded queue using `application.conf` defaults.
   *
   * This loads `queue.bounded.capacity` via
   * `QueueConfig.BoundedQueueConfig.default`.
   *
   * @tparam A
   *   Element type.
   * @return
   *   Bounded lock-free queue instance.
   */
  def bounded[A](): Queue[A] =
    bounded(QueueConfig.BoundedQueueConfig.default)
}
