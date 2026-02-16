package com.example.linkextractor

import com.example.linkextractor.consumer.{ConsumerPool, UrlConsumer}
import com.example.linkextractor.extractor.RegexHyperlinkExtractor
import com.example.linkextractor.io.{Sink, Source}
import com.example.linkextractor.models.{UrlLinksRecord, UrlMarkupRecord}
import com.example.linkextractor.producer.UrlProducerPool
import com.example.linkextractor.queue.{Queue, QueueConfig}
import com.typesafe.config.ConfigFactory
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path}
import scala.collection.mutable.ListBuffer
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class WebLinkExtractorIntegrationSpec extends AnyFunSuite with Matchers with TestSupport {

  test("producer and consumer pools process records end-to-end") {
    val urls   = (1 to 15).map(i => s"https://example.com/$i")
    val source = Source.fromInputs(urls)
    val queue  = Queue.bounded[UrlMarkupRecord](QueueConfig.BoundedQueueConfig(capacity = 128))
    val sink   = new RecordingSink

    val producerPool = UrlProducerPool.withExtractionFunction(
      producerCount = 3,
      extractMarkup = url => Future.successful(s"""<a href="$url/one">one</a><a href="$url/two">two</a>""")
    )

    val consumerPool = new ConsumerPool[UrlMarkupRecord](
      consumerCount = 2,
      consumerFactory = () => UrlConsumer.withExtractor(RegexHyperlinkExtractor.default, sink),
      onShutdown = () => sink.close()
    )

    await(
      producerPool
        .runToCompletion(source, queue)
        .zip(consumerPool.runToCompletion(queue))
        .map(_ => ())
    )

    sink.closed shouldBe true
    sink.records.size shouldBe urls.size
    sink.records.foreach {
      case UrlLinksRecord.Extracted(url, links) =>
        links should contain(s"$url/one")
        links should contain(s"$url/two")
      case other =>
        fail(s"Expected extracted record but got: $other")
    }
  }

  test("runFilePipelineFromConfig completes using file input from config") {
    val file        = writeTempUrls(Seq("://bad-url"))
    val escapedPath = file.toString.replace("\\", "\\\\")

    val config = ConfigFactory.parseString(
      s"""
         |queue.bounded.capacity = 16
         |
         |extractor.http {
         |  connect-timeout-ms = 100
         |  request-timeout-ms = 100
         |  user-agent = "test-agent"
         |}
         |
         |example {
         |  input-file-path = "$escapedPath"
         |  producer-count = 2
         |  consumer-count = 2
         |}
         |
         |runtime {
         |  thread-pool-size = 2
         |  await-timeout-minutes = 1
         |}
         |""".stripMargin
    )

    try {
      await(WebLinkExtractor.runFilePipelineFromConfig(config))
    } finally {
      Files.deleteIfExists(file)
    }
  }

  test("runApplication executes main pipeline lifecycle with custom executor") {
    val file        = writeTempUrls(Seq("://bad-url"))
    val escapedPath = file.toString.replace("\\", "\\\\")

    val config = ConfigFactory.parseString(
      s"""
         |queue.bounded.capacity = 16
         |
         |extractor.http {
         |  connect-timeout-ms = 100
         |  request-timeout-ms = 100
         |  user-agent = "test-agent"
         |}
         |
         |example {
         |  input-file-path = "$escapedPath"
         |  producer-count = 1
         |  consumer-count = 1
         |}
         |
         |runtime {
         |  thread-pool-size = 2
         |  await-timeout-minutes = 1
         |}
         |""".stripMargin
    )

    try {
      noException shouldBe thrownBy(WebLinkExtractor.runApplication(config))
    } finally {
      Files.deleteIfExists(file)
    }
  }

  private def writeTempUrls(urls: Seq[String]): Path = {
    val file = Files.createTempFile("link-extractor-it", ".txt")
    Files.write(file, urls.mkString(System.lineSeparator()).getBytes(StandardCharsets.UTF_8))
    file
  }

  private final class RecordingSink extends Sink {
    val records: ListBuffer[UrlLinksRecord] = ListBuffer.empty
    @volatile var closed: Boolean           = false

    override def emit(result: UrlLinksRecord): Future[Unit] = {
      records += result
      Future.unit
    }

    override def close(): Unit =
      closed = true
  }
}
