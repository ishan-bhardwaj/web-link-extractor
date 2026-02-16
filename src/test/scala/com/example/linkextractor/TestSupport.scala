package com.example.linkextractor

import com.example.linkextractor.queue.Queue

import scala.annotation.tailrec
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

/**
 * Shared test helpers.
 */
trait TestSupport {

  /**
   * Awaits completion of a future.
   *
   * @param future
   *   Future to await.
   * @param atMost
   *   Maximum wait duration.
   * @tparam A
   *   Future result type.
   * @return
   *   Future result.
   */
  protected def await[A](future: Future[A], atMost: FiniteDuration = 5.seconds): A =
    Await.result(future, atMost)

  /**
   * Drains a queue until it reports closed/drained (`None`).
   *
   * @param queue
   *   Queue to drain.
   * @param atMost
   *   Maximum wait duration per dequeue step.
   * @tparam A
   *   Queue element type.
   * @return
   *   Drained values in dequeue order.
   */
  protected def drainQueue[A](queue: Queue[A], atMost: FiniteDuration = 5.seconds): List[A] = {
    @tailrec
    def loop(acc: List[A]): List[A] =
      await(queue.dequeue(), atMost) match {
        case Some(value) => loop(value :: acc)
        case None        => acc.reverse
      }

    loop(Nil)
  }
}
