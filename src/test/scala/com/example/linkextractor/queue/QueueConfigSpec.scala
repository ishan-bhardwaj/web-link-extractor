package com.example.linkextractor.queue

import com.typesafe.config.ConfigFactory
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

class QueueConfigSpec extends AnyFunSuite with Matchers {

  test("load reads bounded queue config from provided config object") {
    val config = ConfigFactory.parseString(
      """
        |queue.bounded.capacity = 32
        |""".stripMargin
    )

    QueueConfig.BoundedQueueConfig.load(config) shouldBe QueueConfig.BoundedQueueConfig(capacity = 32)
  }

  test("load fails when queue.bounded section is missing") {
    val config = ConfigFactory.parseString("other.value = 1")

    intercept[IllegalArgumentException] {
      QueueConfig.BoundedQueueConfig.load(config)
    }
  }

  test("bounded queue config validates positive capacity") {
    intercept[IllegalArgumentException] {
      QueueConfig.BoundedQueueConfig(capacity = 0)
    }
  }
}
