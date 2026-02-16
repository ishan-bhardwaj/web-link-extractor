package com.example.linkextractor.io

import com.example.linkextractor.models.UrlLinksRecord
import com.example.linkextractor.models.UrlLinksRecord.{Extracted, ParseFailed, UpstreamFailed}
import com.example.linkextractor.utils.Logging

import scala.concurrent.Future

/**
 * Output sink for consumer results.
 */
trait Sink {

  /**
   * Emits one consumer result.
   *
   * @param result
   *   Consumer output record.
   * @return
   *   Future completed when emission attempt is done.
   */
  def emit(result: UrlLinksRecord): Future[Unit]

  /**
   * Closes sink resources.
   */
  def close(): Unit = ()
}

object Sink {

  /**
   * Command-line output sink.
   */
  final class Console extends Sink with Logging {

    /**
     * Emits one record to the log output.
     *
     * @param result
     *   Consumer output record.
     * @return
     *   Already-completed future because logging is synchronous.
     */
    override def emit(result: UrlLinksRecord): Future[Unit] = {
      result match {
        case Extracted(url, links) =>
          logger.info(s"URL: $url | links=[${links.mkString(", ")}]")
        case UpstreamFailed(url, errorMessage) =>
          logger.warn(s"URL: $url | upstream fetch failed: $errorMessage")
        case ParseFailed(url, errorMessage) =>
          logger.warn(s"URL: $url | parse failed: $errorMessage")
      }
      Future.unit
    }
  }

  /**
   * Creates command-line output sink.
   *
   * @return
   *   Console sink.
   */
  def console(): Sink = new Console
}
