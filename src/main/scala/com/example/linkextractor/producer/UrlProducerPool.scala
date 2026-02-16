package com.example.linkextractor.producer

import com.example.linkextractor.io.Source
import com.example.linkextractor.models.UrlMarkupRecord
import com.example.linkextractor.queue.Queue
import com.example.linkextractor.utils.Logging
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

/**
 * Orchestrates a fixed-size pool of URL producers.
 *
 * A single shared source stream is consumed by multiple producer workers. Each
 * worker:
 *   1. claims the next URL from the stream,
 *   2. executes producer logic for that URL,
 *   3. repeats until the source is exhausted.
 *
 * The pool owns source/queue lifecycle boundaries:
 *   1. source is always closed when processing completes,
 *   2. queue is always closed so consumers can terminate once drained.
 *
 * @param producerCount
 *   Number of concurrent producer workers.
 * @param producerFactory
 *   Factory used to build one producer instance per worker.
 * @param ec
 *   Execution context used for asynchronous orchestration.
 */
final class UrlProducerPool(
  producerCount: Int,
  producerFactory: () => Producer[UrlMarkupRecord]
)(implicit ec: ExecutionContext)
    extends Logging {
  require(producerCount > 0, "producerCount must be greater than 0")

  /** Synchronizes access to shared source iterator state. */
  private val streamLock = new AnyRef

  /**
   * Starts all producer workers and runs until source exhaustion.
   *
   * @param source
   *   Source of input URLs.
   * @param outputQueue
   *   Queue that receives producer output records.
   * @return
   *   Future completed when all workers stop, after source close and queue
   *   close are attempted.
   */
  def runToCompletion(
    source: Source,
    outputQueue: Queue[UrlMarkupRecord]
  ): Future[Unit] = {
    val stream      = source.stream
    val workerTasks = buildWorkerTasks(stream, outputQueue)
    val runFuture   = startWorkers(workerTasks)

    runFuture.transform(
      success => {
        closeSourceQuietly(source)
        outputQueue.close()
        success
      },
      failure => {
        logger.error(s"Producer pool failed: ${Option(failure.getMessage).getOrElse(failure.toString)}", failure)
        closeSourceQuietly(source)
        outputQueue.close()
        failure
      }
    )
  }

  /**
   * Builds deferred worker functions.
   *
   * Construction is intentionally separated from startup so configuration and
   * lifecycle are explicit.
   *
   * @param stream
   *   Shared URL iterator.
   * @param outputQueue
   *   Queue that receives produced records.
   * @return
   *   One deferred async task per worker.
   */
  private def buildWorkerTasks(
    stream: Iterator[String],
    outputQueue: Queue[UrlMarkupRecord]
  ): Vector[() => Future[Unit]] =
    Vector.fill(producerCount) {
      val producer = producerFactory()
      () => runWorker(producer, stream, outputQueue)
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
   * Worker loop for one producer.
   *
   * @param producer
   *   Worker-local producer instance.
   * @param stream
   *   Shared URL iterator.
   * @param outputQueue
   *   Queue that receives produced records.
   * @return
   *   Future that completes when this worker reaches end of stream.
   */
  private def runWorker(
    producer: Producer[UrlMarkupRecord],
    stream: Iterator[String],
    outputQueue: Queue[UrlMarkupRecord]
  ): Future[Unit] =
    nextUrl(stream).flatMap {
      case Some(url) =>
        producer
          .produce(url, outputQueue)
          .flatMap(_ => runWorker(producer, stream, outputQueue))
      case None =>
        Future.unit
    }

  /**
   * Reads one URL from the shared stream under synchronization.
   *
   * @param stream
   *   Shared URL iterator.
   * @return
   *   `Some(url)` when data exists, otherwise `None` on stream exhaustion.
   */
  private def nextUrl(stream: Iterator[String]): Future[Option[String]] =
    try
      Future.successful {
        streamLock.synchronized {
          if (stream.hasNext) Some(stream.next()) else None
        }
      }
    catch { case NonFatal(error) => Future.failed(error) }

  /**
   * Closes source while suppressing close-time failures.
   *
   * @param source
   *   Source to close.
   */
  private def closeSourceQuietly(source: Source): Unit =
    try source.close()
    catch {
      case NonFatal(error) =>
        logger.warn(s"Failed to close source: ${error.getMessage}")
    }
}

object UrlProducerPool {

  /**
   * Creates a pool from an injected extraction function.
   *
   * @param producerCount
   *   Number of concurrent producer workers.
   * @param extractMarkup
   *   URL-to-markup extraction function.
   * @param ec
   *   Execution context.
   * @return
   *   URL producer pool.
   */
  def withExtractionFunction(
    producerCount: Int,
    extractMarkup: String => Future[String]
  )(implicit ec: ExecutionContext): UrlProducerPool =
    new UrlProducerPool(
      producerCount = producerCount,
      producerFactory = () => new UrlProducer(extractMarkup)
    )
}
