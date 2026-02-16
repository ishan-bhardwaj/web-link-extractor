package com.example.linkextractor.models

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

class ModelSpec extends AnyFunSuite with Matchers {

  test("UrlMarkupRecord smart constructors build expected variants") {
    UrlMarkupRecord.success("u", "<html/>") shouldBe UrlMarkupRecord.MarkupSuccess("u", "<html/>")
    UrlMarkupRecord.failed("u", "err") shouldBe UrlMarkupRecord.MarkupFailed("u", "err")
  }

  test("UrlLinksRecord smart constructors build expected variants") {
    UrlLinksRecord.success("u", Vector("a")) shouldBe UrlLinksRecord.Extracted("u", Vector("a"))
    UrlLinksRecord.failedUpstream("u", "err") shouldBe UrlLinksRecord.UpstreamFailed("u", "err")
    UrlLinksRecord.failedParse("u", "err") shouldBe UrlLinksRecord.ParseFailed("u", "err")
  }
}
