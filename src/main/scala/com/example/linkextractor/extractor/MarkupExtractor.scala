package com.example.linkextractor.extractor

import scala.concurrent.Future

/**
 * Extracts markup for one URL.
 */
trait MarkupExtractor {

  /**
   * Fetches markup for the provided URL.
   *
   * @param url
   *   URL to fetch.
   * @return
   *   Future containing fetched markup.
   */
  def extract(url: String): Future[String]
}
