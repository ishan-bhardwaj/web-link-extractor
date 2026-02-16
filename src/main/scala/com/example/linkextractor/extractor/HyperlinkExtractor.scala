package com.example.linkextractor.extractor

/**
 * Extracts hyperlinks from HTML markup.
 */
trait HyperlinkExtractor {

  /**
   * Extracts all hyperlink targets from one markup document.
   *
   * @param markup
   *   Markup document to parse.
   * @return
   *   Extracted hyperlink targets.
   */
  def extractLinks(markup: String): Vector[String]
}
