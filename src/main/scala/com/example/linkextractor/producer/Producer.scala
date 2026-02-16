package com.example.linkextractor.producer

import com.example.linkextractor.queue.Queue

import scala.concurrent.Future

/**
 * Producer contract for handling one URL at a time.
 *
 * Producer implementations are responsible for URL-specific business logic,
 * while URL stream traversal/orchestration is handled by
 * [[com.example.linkextractor.producer.UrlProducerPool]].
 *
 * @tparam A
 *   Output record type emitted into the queue.
 */
trait Producer[A] {

  /**
   * Processes one URL and enqueues its result.
   *
   * @param url
   *   Input URL to process.
   * @param outputQueue
   *   Queue receiving producer output.
   * @return
   *   Future completed when processing and enqueue handling for this URL are
   *   done.
   */
  def produce(url: String, outputQueue: Queue[A]): Future[Unit]
}
