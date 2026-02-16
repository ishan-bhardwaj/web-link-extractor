package com.example.linkextractor.io

import com.example.linkextractor.TestSupport
import com.example.linkextractor.models.UrlLinksRecord
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

class SinkSpec extends AnyFunSuite with Matchers with TestSupport {

  test("console sink emits all UrlLinksRecord variants") {
    val sink = Sink.console()

    await(sink.emit(UrlLinksRecord.success("https://a.example", Vector("https://link.example"))))
    await(sink.emit(UrlLinksRecord.failedUpstream("https://b.example", "fetch failed")))
    await(sink.emit(UrlLinksRecord.failedParse("https://c.example", "parse failed")))

    noException shouldBe thrownBy(sink.close())
  }
}
