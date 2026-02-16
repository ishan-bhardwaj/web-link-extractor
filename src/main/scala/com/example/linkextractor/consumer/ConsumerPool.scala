package com.example.linkextractor.consumer

import com.example.linkextractor.queue.Queue
import com.example.linkextractor.utils.Logging
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

/**
 * Orchestrates a fixed-size pool of queue consumers.
 *
 * Each worker repeatedly dequeues from the same shared queue and delegates
 * record handling to a consumer instance.
 *
 * Workers stop naturally when `queue.dequeue()` returns `None`, which indicates
 * the queue is closed and drained.
 *
 * @param consumerCount
 *   Number of concurrent consumer workers.
 * @param consumerFactory
 *   Factory used to build one consumer instance per worker.
 * @param onShutdown
 *   Finalizer executed exactly once after pool completion/failure.
 * @param ec
 *   Execution context used for asynchronous orchestration.
 * @tparam A
 *   Queue element type consumed by this pool.
 */
final class ConsumerPool[A](
  consumerCount: Int,
  consumerFactory: () => Consumer[A],
  onShutdown: () => Unit = () => ()
)(implicit ec: ExecutionContext)
    extends Logging {
  require(consumerCount > 0, "consumerCount must be greater than 0")

  /**
   * Starts all consumer workers and runs until queue closure + drain.
   *
   * @param inputQueue
   *   Shared queue consumed by all workers.
   * @return
   *   Future completed when all workers finish and shutdown finalizer executes.
   */
  def runToCompletion(inputQueue: Queue[A]): Future[Unit] = {
    val tasks     = buildWorkerTasks(inputQueue)
    val runFuture = startWorkers(tasks)

    runFuture.transform(
      success => {
        shutdownQuietly()
        success
      },
      failure => {
        logger.error(s"Consumer pool failed: ${Option(failure.getMessage).getOrElse(failure.toString)}", failure)
        shutdownQuietly()
        failure
      }
    )
  }

  /**
   * Builds deferred worker functions.
   *
   * @param inputQueue
   *   Shared queue consumed by all workers.
   * @return
   *   One deferred async task per worker.
   */
  private def buildWorkerTasks(inputQueue: Queue[A]): Vector[() => Future[Unit]] =
    Vector.fill(consumerCount) {
      val consumer = consumerFactory()
      () => runWorker(consumer, inputQueue)
    }

  /**
   * Starts all worker tasks concurrently.
   *
   * @param tasks
   *   Deferred worker tasks.
   * @return
   *   Future that completes when all workers complete.
   */
  private def startWorkers(tasks: Vector[() => Future[Unit]]): Future[Unit] =
    Future.sequence(tasks.map(start => start())).map(_ => ())

  /**
   * Worker loop for one consumer.
   *
   * @param consumer
   *   Worker-local consumer instance.
   * @param inputQueue
   *   Shared input queue.
   * @return
   *   Future that completes when this worker observes queue termination.
   */
  private def runWorker(consumer: Consumer[A], inputQueue: Queue[A]): Future[Unit] =
    inputQueue.dequeue().flatMap {
      case Some(record) =>
        consumer.consume(record).flatMap(_ => runWorker(consumer, inputQueue))
      case None =>
        Future.unit
    }

  /**
   * Executes pool finalizer while suppressing finalizer failures.
   */
  private def shutdownQuietly(): Unit =
    try onShutdown()
    catch {
      case NonFatal(error) =>
        logger.warn(s"Consumer pool shutdown hook failed: ${error.getMessage}")
    }
}
