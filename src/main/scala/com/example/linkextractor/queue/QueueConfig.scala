package com.example.linkextractor.queue

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory

/**
 * Base configuration marker for queue implementations.
 */
sealed trait QueueConfig

object QueueConfig {

  /**
   * Configuration for bounded queues.
   *
   * @param capacity
   *   Maximum number of buffered elements retained in memory.
   */
  final case class BoundedQueueConfig(capacity: Int) extends QueueConfig {
    require(capacity > 0, "capacity must be greater than 0")
  }

  object BoundedQueueConfig {
    private val SectionPath = "queue.bounded"

    /**
     * Loads bounded queue configuration from a Typesafe Config instance.
     *
     * Expected path: `queue.bounded.capacity`.
     *
     * @param config
     *   Source configuration.
     * @return
     *   Validated bounded queue configuration.
     */
    def load(config: Config): BoundedQueueConfig = {
      val section =
        if (config.hasPath(SectionPath)) config.getConfig(SectionPath)
        else throw new IllegalArgumentException(s"Missing config section: $SectionPath")

      BoundedQueueConfig(
        capacity = section.getInt("capacity")
      )
    }

    /**
     * Loads bounded queue configuration from `application.conf` and standard
     * Typesafe fallback sources.
     *
     * @return
     *   Validated bounded queue configuration.
     */
    def load(): BoundedQueueConfig =
      load(ConfigFactory.load())

    /**
     * Lazily cached default bounded queue configuration from
     * `application.conf`.
     *
     * @return
     *   Validated bounded queue configuration.
     */
    lazy val default: BoundedQueueConfig = load()
  }
}
