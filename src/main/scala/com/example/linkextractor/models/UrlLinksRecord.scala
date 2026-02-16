package com.example.linkextractor.models

/**
 * Consumer-side output record for one URL.
 */
sealed trait UrlLinksRecord {

  /**
   * URL associated with this record.
   *
   * @return
   *   Source URL.
   */
  def url: String
}

object UrlLinksRecord {

  /**
   * Hyperlinks extracted successfully for a URL.
   *
   * @param url
   *   Source URL.
   * @param links
   *   Extracted hyperlinks.
   */
  final case class Extracted(
    url: String,
    links: Vector[String]
  ) extends UrlLinksRecord

  /**
   * Producer could not fetch markup for the URL.
   *
   * @param url
   *   Source URL.
   * @param errorMessage
   *   Upstream failure message.
   */
  final case class UpstreamFailed(
    url: String,
    errorMessage: String
  ) extends UrlLinksRecord

  /**
   * Consumer failed to parse hyperlinks for the URL.
   *
   * @param url
   *   Source URL.
   * @param errorMessage
   *   Parse failure message.
   */
  final case class ParseFailed(
    url: String,
    errorMessage: String
  ) extends UrlLinksRecord

  /**
   * Creates a successful extraction record.
   *
   * @param url
   *   Source URL.
   * @param links
   *   Extracted hyperlinks.
   * @return
   *   Successful link extraction record.
   */
  def success(url: String, links: Vector[String]): UrlLinksRecord =
    Extracted(url = url, links = links)

  /**
   * Creates an upstream-failure record.
   *
   * @param url
   *   Source URL.
   * @param errorMessage
   *   Upstream failure message.
   * @return
   *   Upstream-failure record.
   */
  def failedUpstream(url: String, errorMessage: String): UrlLinksRecord =
    UpstreamFailed(url = url, errorMessage = errorMessage)

  /**
   * Creates a parse-failure record.
   *
   * @param url
   *   Source URL.
   * @param errorMessage
   *   Parse failure message.
   * @return
   *   Parse-failure record.
   */
  def failedParse(url: String, errorMessage: String): UrlLinksRecord =
    ParseFailed(url = url, errorMessage = errorMessage)
}
