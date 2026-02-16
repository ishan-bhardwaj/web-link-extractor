package com.example.linkextractor.consumer

import com.example.linkextractor.extractor.HyperlinkExtractor
import com.example.linkextractor.io.Sink
import com.example.linkextractor.models.{UrlLinksRecord, UrlMarkupRecord}
import com.example.linkextractor.utils.Logging

import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

/**
 * URL consumer that parses fetched markup and emits hyperlink results.
 *
 * @param extractLinks
 *   hyperlink extraction function
 * @param output
 *   output sink
 * @param ec
 *   execution context
 */
final class UrlConsumer(
  extractLinks: String => Vector[String],
  output: Sink
)(implicit ec: ExecutionContext)
    extends Consumer[UrlMarkupRecord]
    with Logging {

  /**
   * Consumes one URL record with failure isolation.
   *
   * @param record
   *   Producer output record for one URL.
   * @return
   *   Future completed when the derived consumer result is emitted.
   */
  override def consume(record: UrlMarkupRecord): Future[Unit] =
    toOutputRecord(record)
      .flatMap(emitSafely)

  /**
   * Converts one producer record into consumer output record.
   *
   * @param record
   *   Producer output record for one URL.
   * @return
   *   Future containing exactly one consumer output record.
   */
  private def toOutputRecord(record: UrlMarkupRecord): Future[UrlLinksRecord] =
    record match {
      case UrlMarkupRecord.MarkupSuccess(url, markup) =>
        Future {
          UrlLinksRecord.success(url = url, links = extractLinks(markup))
        }.recover { case NonFatal(error) =>
          val errorMessage = Option(error.getMessage).getOrElse(error.toString)
          UrlLinksRecord.failedParse(url = url, errorMessage = errorMessage)
        }
      case UrlMarkupRecord.MarkupFailed(url, errorMessage) =>
        Future.successful(UrlLinksRecord.failedUpstream(url = url, errorMessage = errorMessage))
    }

  /**
   * Emits one output record and suppresses sink failures per-item.
   *
   * @param result
   *   Consumer output record.
   * @return
   *   Future that always succeeds after a best-effort emission attempt.
   */
  private def emitSafely(result: UrlLinksRecord): Future[Unit] =
    try {
      output.emit(result).recover { case NonFatal(error) =>
        logger.error(s"Consumer output failed for url [${result.url}]", error)
      }
    } catch {
      case NonFatal(error) =>
        logger.error(s"Consumer output failed for url [${result.url}]", error)
        Future.unit
    }
}

object UrlConsumer {

  /**
   * Builds a URL consumer from an injected hyperlink extractor and sink.
   *
   * @param extractor
   *   Hyperlink extractor implementation.
   * @param output
   *   Output sink implementation.
   * @param ec
   *   Execution context.
   * @return
   *   URL consumer instance.
   */
  def withExtractor(
    extractor: HyperlinkExtractor,
    output: Sink
  )(implicit ec: ExecutionContext): UrlConsumer =
    new UrlConsumer(extractLinks = extractor.extractLinks, output = output)
}
