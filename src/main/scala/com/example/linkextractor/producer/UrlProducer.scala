package com.example.linkextractor.producer

import com.example.linkextractor.extractor.MarkupExtractor
import com.example.linkextractor.utils.Logging
import com.example.linkextractor.models.UrlMarkupRecord
import com.example.linkextractor.queue.Queue

import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

/**
 * Simple producer implementation that delegates fetching to an injected
 * extraction function.
 *
 * @param extractMarkup
 *   Function that fetches markup for a URL.
 * @param ec
 *   Execution context for async operations.
 */
final class UrlProducer(
  extractMarkup: String => Future[String]
)(implicit ec: ExecutionContext)
    extends Producer[UrlMarkupRecord]
    with Logging {

  /**
   * Processes one URL and enqueues either success or failure.
   *
   * Error isolation rule:
   *   1. fetch errors are transformed into `UrlMarkupRecord.MarkupFailed`,
   *   2. queue write outcomes are handled explicitly,
   *   3. method never fails for per-URL business errors.
   *
   * @param url
   *   URL to fetch.
   * @param outputQueue
   *   Queue receiving markup records.
   * @return
   *   Future completed once the URL has been processed and enqueue handling has
   *   finished.
   */
  override def produce(url: String, outputQueue: Queue[UrlMarkupRecord]): Future[Unit] =
    safeExtract(url)
      .map(markup => UrlMarkupRecord.success(url = url, markup = markup))
      .recover { case NonFatal(error) =>
        val errorMessage = Option(error.getMessage).getOrElse(error.toString)
        logger.error(s"Markup extraction failed for url [$url]: $errorMessage", error)
        UrlMarkupRecord.failed(url = url, errorMessage = errorMessage)
      }
      .map { record =>
        outputQueue.enqueue(record) match {
          case Queue.EnqueueResult.Enqueued =>
            ()
          case Queue.EnqueueResult.EnqueuedAfterDroppingOldest =>
            logger.warn(s"Queue full while enqueueing record for url [$url]; dropped oldest queue entry")
          case Queue.EnqueueResult.Closed =>
            logger.warn(s"Queue already closed; dropped record for url [$url]")
        }
      }

  /**
   * Wraps extractor invocation so synchronous throws become failed futures.
   *
   * @param url
   *   URL to extract markup from.
   * @return
   *   Future with extracted markup, or failed future when extraction throws.
   */
  private def safeExtract(url: String): Future[String] =
    try extractMarkup(url)
    catch { case NonFatal(error) => Future.failed(error) }

}

object UrlProducer {

  /**
   * Builds a URL producer with a `MarkupExtractor`.
   *
   * @param extractor
   *   Markup extractor implementation.
   * @param ec
   *   Execution context.
   * @return
   *   URL producer instance.
   */
  def withExtractor(extractor: MarkupExtractor)(implicit ec: ExecutionContext): UrlProducer =
    new UrlProducer(extractor.extract)
}
