package com.example.linkextractor.models

/**
 * A queue record produced for a URL.
 */
sealed trait UrlMarkupRecord {

  /**
   * URL associated with this record.
   *
   * @return
   *   Source URL.
   */
  def url: String
}

object UrlMarkupRecord {

  /**
   * URL was fetched successfully.
   *
   * @param url
   *   Source URL.
   * @param markup
   *   Retrieved markup payload.
   */
  final case class MarkupSuccess(
    url: String,
    markup: String
  ) extends UrlMarkupRecord

  /**
   * URL fetch failed.
   *
   * @param url
   *   Source URL.
   * @param errorMessage
   *   Failure message.
   */
  final case class MarkupFailed(
    url: String,
    errorMessage: String
  ) extends UrlMarkupRecord

  /**
   * Creates a success record.
   *
   * @param url
   *   Source URL.
   * @param markup
   *   Retrieved markup payload.
   * @return
   *   Successful markup record.
   */
  def success(url: String, markup: String): UrlMarkupRecord =
    MarkupSuccess(url = url, markup = markup)

  /**
   * Creates a failed record.
   *
   * @param url
   *   Source URL.
   * @param errorMessage
   *   Failure message.
   * @return
   *   Failed markup record.
   */
  def failed(url: String, errorMessage: String): UrlMarkupRecord =
    MarkupFailed(url = url, errorMessage = errorMessage)

}
