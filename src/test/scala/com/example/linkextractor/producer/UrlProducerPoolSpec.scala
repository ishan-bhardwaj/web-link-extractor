package com.example.linkextractor.producer

import com.example.linkextractor.TestSupport
import com.example.linkextractor.io.Source
import com.example.linkextractor.models.UrlMarkupRecord
import com.example.linkextractor.queue.{Queue, QueueConfig}
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

import java.util.concurrent.ConcurrentLinkedQueue
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.jdk.CollectionConverters._

class UrlProducerPoolSpec extends AnyFunSuite with Matchers with TestSupport {

  test("runToCompletion processes every URL and closes source and queue") {
    val urls   = (1 to 20).map(i => s"https://example.com/$i")
    val source = new TestSource(urls.iterator)
    val queue  = Queue.bounded[UrlMarkupRecord](QueueConfig.BoundedQueueConfig(capacity = 64))

    val seen = new ConcurrentLinkedQueue[String]()
    val pool = new UrlProducerPool(
      producerCount = 3,
      producerFactory = () =>
        (url: String, outputQueue: Queue[UrlMarkupRecord]) => Future {
          seen.add(url)
          outputQueue.enqueue(UrlMarkupRecord.success(url, s"<html>$url</html>"))
          ()
        }
    )

    await(pool.runToCompletion(source, queue))

    source.closed shouldBe true

    val drainedUrls = drainQueue(queue).collect { case UrlMarkupRecord.MarkupSuccess(url, _) => url }.toSet
    drainedUrls shouldBe urls.toSet
    seen.asScala.toSet shouldBe urls.toSet
  }

  test("runToCompletion closes source and queue on worker failure") {
    val source = new TestSource(Iterator("one", "two"))
    val queue  = Queue.bounded[UrlMarkupRecord](QueueConfig.BoundedQueueConfig(capacity = 8))

    val pool = new UrlProducerPool(
      producerCount = 1,
      producerFactory = () =>
        (url: String, _: Queue[UrlMarkupRecord]) => if (url == "one") Future.failed(new RuntimeException("worker failure"))
        else Future.unit
    )

    val failure = intercept[RuntimeException] {
      await(pool.runToCompletion(source, queue))
    }
    failure.getMessage should include("worker failure")

    source.closed shouldBe true
    await(queue.dequeue()) shouldBe None
  }

  private final class TestSource(values: Iterator[String]) extends Source {
    @volatile var closed: Boolean = false

    override val stream: Iterator[String] = values

    override def close(): Unit =
      closed = true
  }
}
