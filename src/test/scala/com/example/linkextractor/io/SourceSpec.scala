package com.example.linkextractor.io

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import scala.jdk.CollectionConverters._

class SourceSpec extends AnyFunSuite with Matchers {

  test("fromInputs normalizes values by trimming and removing blanks/comments") {
    val source = Source.fromInputs(
      List(
        " https://a.example ",
        "",
        "   ",
        "# comment",
        "https://b.example"
      )
    )

    try {
      source.stream.toList shouldBe List("https://a.example", "https://b.example")
    } finally {
      source.close()
    }
  }

  test("fromFile reads and normalizes newline-delimited URLs") {
    val file = Files.createTempFile("source-spec", ".txt")
    Files.write(
      file,
      List(
        "https://first.example",
        "",
        "  https://second.example  ",
        "# ignored"
      ).asJava,
      StandardCharsets.UTF_8
    )

    val source = Source.fromFile(file)
    try {
      source.stream.toList shouldBe List("https://first.example", "https://second.example")
    } finally {
      source.close()
      Files.deleteIfExists(file)
    }
  }
}
