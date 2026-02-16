package com.example.linkextractor.producer

import com.example.linkextractor.TestSupport
import com.example.linkextractor.models.UrlMarkupRecord
import com.example.linkextractor.queue.Queue
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

import scala.collection.mutable.ListBuffer
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class UrlProducerSpec extends AnyFunSuite with Matchers with TestSupport {

  test("produce enqueues successful markup record") {
    val queue    = new RecordingQueue
    val producer = new UrlProducer(url => Future.successful(s"<html>$url</html>"))

    await(producer.produce("https://a.example", queue))

    queue.enqueued.toList shouldBe List(
      UrlMarkupRecord.success("https://a.example", "<html>https://a.example</html>")
    )
  }

  test("produce converts extractor failure to failed markup record") {
    val queue    = new RecordingQueue
    val producer = new UrlProducer(_ => Future.failed(new RuntimeException("boom")))

    await(producer.produce("https://a.example", queue))

    queue.enqueued.size shouldBe 1
    queue.enqueued.head match {
      case UrlMarkupRecord.MarkupFailed(url, errorMessage) =>
        url shouldBe "https://a.example"
        errorMessage should include("boom")
      case other =>
        fail(s"Expected MarkupFailed but got: $other")
    }
  }

  test("produce converts synchronous extractor throw to failed markup record") {
    val queue    = new RecordingQueue
    val producer = new UrlProducer(_ => throw new IllegalArgumentException("bad url"))

    await(producer.produce("not-a-url", queue))

    queue.enqueued.head match {
      case UrlMarkupRecord.MarkupFailed(url, errorMessage) =>
        url shouldBe "not-a-url"
        errorMessage should include("bad url")
      case other =>
        fail(s"Expected MarkupFailed but got: $other")
    }
  }

  test("produce handles closed queue result without failing the future") {
    val queue = new RecordingQueue
    queue.enqueueResult = Queue.EnqueueResult.Closed
    val producer = new UrlProducer(_ => Future.successful("<html/>"))

    await(producer.produce("https://a.example", queue)) shouldBe ()
    queue.enqueued.size shouldBe 1
  }

  private final class RecordingQueue extends Queue[UrlMarkupRecord] {
    val enqueued: ListBuffer[UrlMarkupRecord] = ListBuffer.empty
    var enqueueResult: Queue.EnqueueResult    = Queue.EnqueueResult.Enqueued

    override def enqueue(value: UrlMarkupRecord): Queue.EnqueueResult = {
      enqueued += value
      enqueueResult
    }

    override def dequeue(): Future[Option[UrlMarkupRecord]] =
      Future.successful(None)

    override def close(): Unit = ()
  }
}
