package com.example.linkextractor.consumer

import com.example.linkextractor.TestSupport
import com.example.linkextractor.queue.{Queue, QueueConfig}
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.jdk.CollectionConverters._

class ConsumerPoolSpec extends AnyFunSuite with Matchers with TestSupport {

  test("runToCompletion consumes all queued values and executes shutdown hook") {
    val queue = Queue.bounded[Int](QueueConfig.BoundedQueueConfig(capacity = 16))
    (1 to 10).foreach(queue.enqueue)
    queue.close()

    val consumed     = new ConcurrentLinkedQueue[Int]()
    val shutDown     = new AtomicBoolean(false)
    val consumerPool = new ConsumerPool[Int](
      consumerCount = 3,
      consumerFactory = () =>
        (record: Int) =>
          Future {
            consumed.add(record)
            ()
          },
      onShutdown = () => shutDown.set(true)
    )

    await(consumerPool.runToCompletion(queue))

    consumed.asScala.toSet shouldBe (1 to 10).toSet
    shutDown.get() shouldBe true
  }

  test("runToCompletion executes shutdown hook on failure and returns failed future") {
    val queue = Queue.bounded[Int](QueueConfig.BoundedQueueConfig(capacity = 4))
    queue.enqueue(1)
    queue.close()

    val shutDown = new AtomicBoolean(false)
    val pool     = new ConsumerPool[Int](
      consumerCount = 1,
      consumerFactory = () => (_: Int) => Future.failed(new RuntimeException("consumer failed")),
      onShutdown = () => shutDown.set(true)
    )

    val failure = intercept[RuntimeException] {
      await(pool.runToCompletion(queue))
    }

    failure.getMessage should include("consumer failed")
    shutDown.get() shouldBe true
  }
}
