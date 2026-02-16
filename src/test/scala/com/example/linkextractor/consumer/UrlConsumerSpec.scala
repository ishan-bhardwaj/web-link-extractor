package com.example.linkextractor.consumer

import com.example.linkextractor.TestSupport
import com.example.linkextractor.io.Sink
import com.example.linkextractor.models.{UrlLinksRecord, UrlMarkupRecord}
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

import scala.collection.mutable.ListBuffer
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class UrlConsumerSpec extends AnyFunSuite with Matchers with TestSupport {

  test("consume converts markup success into extracted links output") {
    val sink     = new RecordingSink
    val consumer = new UrlConsumer(
      extractLinks = _ => Vector("https://a.example", "https://b.example"),
      output = sink
    )

    await(consumer.consume(UrlMarkupRecord.success("https://host.example", "<html/>")))

    sink.records.toList shouldBe List(
      UrlLinksRecord.success("https://host.example", Vector("https://a.example", "https://b.example"))
    )
  }

  test("consume converts parse failure into parse-failed output") {
    val sink     = new RecordingSink
    val consumer = new UrlConsumer(
      extractLinks = _ => throw new RuntimeException("parse error"),
      output = sink
    )

    await(consumer.consume(UrlMarkupRecord.success("https://host.example", "<html/>")))

    sink.records.size shouldBe 1
    sink.records.head match {
      case UrlLinksRecord.ParseFailed(url, errorMessage) =>
        url shouldBe "https://host.example"
        errorMessage should include("parse error")
      case other =>
        fail(s"Expected ParseFailed but got: $other")
    }
  }

  test("consume maps upstream producer failure into upstream-failed output") {
    val sink     = new RecordingSink
    val consumer = new UrlConsumer(_ => Vector.empty, sink)

    await(consumer.consume(UrlMarkupRecord.failed("https://bad.example", "fetch failed")))

    sink.records.toList shouldBe List(
      UrlLinksRecord.failedUpstream("https://bad.example", "fetch failed")
    )
  }

  test("consume suppresses sink emission failures per record") {
    val sink     = new RecordingSink(failEmit = true)
    val consumer = new UrlConsumer(
      extractLinks = _ => Vector("https://x.example"),
      output = sink
    )

    await(consumer.consume(UrlMarkupRecord.success("https://host.example", "<html/>"))) shouldBe ()
  }

  private final class RecordingSink(failEmit: Boolean = false) extends Sink {
    val records: ListBuffer[UrlLinksRecord] = ListBuffer.empty

    override def emit(result: UrlLinksRecord): Future[Unit] = {
      records += result
      if (failEmit) Future.failed(new RuntimeException("sink failed")) else Future.unit
    }
  }
}
