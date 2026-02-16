package com.example.linkextractor.extractor

/**
 * Regex-based hyperlink extractor for HTML-like markup.
 *
 * This extractor is intentionally lightweight and dependency-free.
 */
final class RegexHyperlinkExtractor extends HyperlinkExtractor {
  private val hrefPattern =
    "(?i)href\\s*=\\s*(\"([^\"]*)\"|'([^']*)'|([^\\s>]+))".r

  /**
   * Extracts hyperlink values from `href=...` attributes.
   *
   * @param markup
   *   Markup document to parse.
   * @return
   *   Extracted hyperlink targets in match order.
   */
  override def extractLinks(markup: String): Vector[String] =
    hrefPattern
      .findAllMatchIn(markup)
      .map { m =>
        Option(m.group(2))
          .orElse(Option(m.group(3)))
          .orElse(Option(m.group(4)))
          .getOrElse("")
          .trim
      }
      .filter(_.nonEmpty)
      .toVector
}

object RegexHyperlinkExtractor {

  /**
   * Default extractor instance.
   *
   * @return
   *   Regex hyperlink extractor.
   */
  val default: RegexHyperlinkExtractor = new RegexHyperlinkExtractor
}
