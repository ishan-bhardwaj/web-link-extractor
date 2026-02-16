package com.example.linkextractor.consumer

import scala.concurrent.Future

/**
 * Consumer contract for handling one queue record at a time.
 *
 * Queue traversal/orchestration is intentionally handled by
 * [[com.example.linkextractor.consumer.ConsumerPool]], while implementations of
 * this trait focus only on per-record logic.
 *
 * @tparam A
 *   Input record type.
 */
trait Consumer[-A] {

  /**
   * Consumes one input record.
   *
   * @param record
   *   Input record to process.
   * @return
   *   Future completed when processing of this record is done.
   */
  def consume(record: A): Future[Unit]
}
