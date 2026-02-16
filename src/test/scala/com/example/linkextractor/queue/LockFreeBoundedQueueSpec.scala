package com.example.linkextractor.queue

import com.example.linkextractor.TestSupport
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

class LockFreeBoundedQueueSpec extends AnyFunSuite with Matchers with TestSupport {

  test("enqueue and dequeue preserve FIFO order") {
    val queue = Queue.bounded[Int](QueueConfig.BoundedQueueConfig(capacity = 4))

    queue.enqueue(1) shouldBe Queue.EnqueueResult.Enqueued
    queue.enqueue(2) shouldBe Queue.EnqueueResult.Enqueued
    queue.enqueue(3) shouldBe Queue.EnqueueResult.Enqueued
    queue.close()

    drainQueue(queue) shouldBe List(1, 2, 3)
  }

  test("enqueue drops oldest element when capacity is full") {
    val queue = Queue.bounded[Int](QueueConfig.BoundedQueueConfig(capacity = 2))

    queue.enqueue(1) shouldBe Queue.EnqueueResult.Enqueued
    queue.enqueue(2) shouldBe Queue.EnqueueResult.Enqueued
    queue.enqueue(3) shouldBe Queue.EnqueueResult.EnqueuedAfterDroppingOldest
    queue.close()

    drainQueue(queue) shouldBe List(2, 3)
  }

  test("dequeue waits when queue is empty and receives a future enqueue") {
    val queue = Queue.bounded[Int](QueueConfig.BoundedQueueConfig(capacity = 4))

    val waiting = queue.dequeue()
    waiting.isCompleted shouldBe false

    queue.enqueue(42) shouldBe Queue.EnqueueResult.Enqueued
    await(waiting) shouldBe Some(42)

    queue.close()
    await(queue.dequeue()) shouldBe None
  }

  test("close completes pending waiters with None when no buffered values remain") {
    val queue = Queue.bounded[Int](QueueConfig.BoundedQueueConfig(capacity = 2))

    val waiting = queue.dequeue()
    waiting.isCompleted shouldBe false

    queue.close()

    await(waiting) shouldBe None
    await(queue.dequeue()) shouldBe None
  }

  test("close is idempotent and rejects future enqueues") {
    val queue = Queue.bounded[Int](QueueConfig.BoundedQueueConfig(capacity = 2))

    queue.enqueue(7) shouldBe Queue.EnqueueResult.Enqueued
    queue.close()
    queue.close()

    queue.enqueue(8) shouldBe Queue.EnqueueResult.Closed

    await(queue.dequeue()) shouldBe Some(7)
    await(queue.dequeue()) shouldBe None
  }
}
