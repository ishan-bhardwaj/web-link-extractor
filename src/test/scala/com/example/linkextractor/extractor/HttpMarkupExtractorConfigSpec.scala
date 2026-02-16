package com.example.linkextractor.extractor

import com.typesafe.config.ConfigFactory
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

class HttpMarkupExtractorConfigSpec extends AnyFunSuite with Matchers {

  test("load reads extractor config from provided config object") {
    val config = ConfigFactory.parseString(
      """
        |extractor.http {
        |  connect-timeout-ms = 1000
        |  request-timeout-ms = 2000
        |  user-agent = "agent"
        |}
        |""".stripMargin
    )

    HttpMarkupExtractorConfig.load(config) shouldBe HttpMarkupExtractorConfig(
      connectTimeoutMs = 1000,
      requestTimeoutMs = 2000,
      userAgent = "agent"
    )
  }

  test("load fails when extractor.http section is missing") {
    val config = ConfigFactory.parseString("other.value = 1")

    intercept[IllegalArgumentException] {
      HttpMarkupExtractorConfig.load(config)
    }
  }

  test("case class validates positive timeouts and non-empty user-agent") {
    intercept[IllegalArgumentException] {
      HttpMarkupExtractorConfig(connectTimeoutMs = 0, requestTimeoutMs = 1000, userAgent = "agent")
    }

    intercept[IllegalArgumentException] {
      HttpMarkupExtractorConfig(connectTimeoutMs = 1000, requestTimeoutMs = -1, userAgent = "agent")
    }

    intercept[IllegalArgumentException] {
      HttpMarkupExtractorConfig(connectTimeoutMs = 1000, requestTimeoutMs = 1000, userAgent = " ")
    }
  }
}
