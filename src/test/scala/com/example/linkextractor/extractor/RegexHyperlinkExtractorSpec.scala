package com.example.linkextractor.extractor

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

class RegexHyperlinkExtractorSpec extends AnyFunSuite with Matchers {

  test("extractLinks parses double-quoted single-quoted and unquoted href values") {
    val extractor = new RegexHyperlinkExtractor
    val markup    =
      """
        |<a href="https://a.example">A</a>
        |<a HREF='https://b.example'>B</a>
        |<a href=https://c.example>C</a>
        |<a data-id="no-link">No Link</a>
        |""".stripMargin

    extractor.extractLinks(markup) shouldBe Vector(
      "https://a.example",
      "https://b.example",
      "https://c.example"
    )
  }

  test("extractLinks returns empty vector when markup has no links") {
    val extractor = new RegexHyperlinkExtractor

    extractor.extractLinks("<html><body>none</body></html>") shouldBe Vector.empty
  }
}
