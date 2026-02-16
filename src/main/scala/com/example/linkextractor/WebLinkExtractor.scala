package com.example.linkextractor

import com.example.linkextractor.consumer.{ConsumerPool, UrlConsumer}
import com.example.linkextractor.extractor.{HttpMarkupExtractor, HttpMarkupExtractorConfig, RegexHyperlinkExtractor}
import com.example.linkextractor.io.{Sink, Source}
import com.example.linkextractor.models.UrlMarkupRecord
import com.example.linkextractor.producer.{UrlProducer, UrlProducerPool}
import com.example.linkextractor.queue.{Queue, QueueConfig}
import com.example.linkextractor.utils.Logging
import com.typesafe.config.{Config, ConfigFactory}

import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.{Executors, ThreadFactory, TimeUnit}
import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, ExecutionContext, ExecutionContextExecutorService, Future}
import scala.util.control.NonFatal

/**
 * Application entry and orchestration utilities for the producer/consumer web
 * link extractor.
 */
object WebLinkExtractor extends Logging {

  private val ExampleSectionPath = "example"
  private val RuntimeSectionPath = "runtime"

  /**
   * Builds and runs the full file-based pipeline:
   *   1. reads URLs from source file,
   *   2. produces markup records concurrently,
   *   3. consumes markup records concurrently and emits extracted links.
   *
   * @param config
   *   Application configuration.
   * @param ec
   *   Execution context.
   * @return
   *   Future completed when both producer and consumer pools terminate.
   */
  def runFilePipelineFromConfig(config: Config)(implicit ec: ExecutionContext): Future[Unit] =
    try {
      val section       = config.getConfig(ExampleSectionPath)
      val inputFilePath = section.getString("input-file-path")
      val producerCount = section.getInt("producer-count")
      val consumerCount = section.getInt("consumer-count")

      val source      = Source.fromFile(inputFilePath)
      val outputQueue = Queue.bounded[UrlMarkupRecord](QueueConfig.BoundedQueueConfig.load(config))
      val sink        = Sink.console()

      val extractor    = HttpMarkupExtractor.fromConfig(HttpMarkupExtractorConfig.load(config))
      val producerPool = new UrlProducerPool(
        producerCount = producerCount,
        producerFactory = () => UrlProducer.withExtractor(extractor)
      )

      val consumerPool = new ConsumerPool[UrlMarkupRecord](
        consumerCount = consumerCount,
        consumerFactory = () => UrlConsumer.withExtractor(RegexHyperlinkExtractor.default, sink),
        onShutdown = () => sink.close()
      )

      val produceFuture = producerPool.runToCompletion(source, outputQueue)
      val consumeFuture = consumerPool.runToCompletion(outputQueue)

      produceFuture.zip(consumeFuture).map(_ => ())
    } catch {
      case NonFatal(error) => Future.failed(error)
    }

  /**
   * Runs the application lifecycle using a custom fixed-size thread pool from
   * configuration.
   *
   * @param config
   *   Application configuration.
   * @return
   *   Unit when execution completes or throws on pipeline failure.
   *
   * Expected runtime keys:
   *   1. `runtime.thread-pool-size`
   *   2. `runtime.await-timeout-minutes`
   */
  def runApplication(config: Config): Unit = {
    val runtimeSection = config.getConfig(RuntimeSectionPath)
    val threadPoolSize = runtimeSection.getInt("thread-pool-size")
    val timeoutMinutes = runtimeSection.getInt("await-timeout-minutes")

    require(threadPoolSize > 0, "runtime.thread-pool-size must be greater than 0")
    require(timeoutMinutes > 0, "runtime.await-timeout-minutes must be greater than 0")

    val executor                                     = newNamedFixedThreadPool(threadPoolSize, "link-extractor-worker")
    implicit val ec: ExecutionContextExecutorService = ExecutionContext.fromExecutorService(executor)

    try {
      Await.result(runFilePipelineFromConfig(config), timeoutMinutes.minutes)
    } finally {
      shutdownExecutor(ec, timeoutMinutes)
    }
  }

  /**
   * Main entry point.
   *
   * Delegates execution to [[runApplication]] so runtime wiring is testable and
   * reusable.
   *
   * @param args
   *   Command-line arguments (currently unused).
   * @return
   *   Unit.
   */
  def main(args: Array[String]): Unit = {
    val config = ConfigFactory.load()
    runApplication(config)
  }

  /**
   * Creates a fixed-size executor with deterministic thread names.
   *
   * @param size
   *   Number of threads.
   * @param threadNamePrefix
   *   Prefix used when naming worker threads.
   * @return
   *   Fixed-size executor service.
   */
  private def newNamedFixedThreadPool(size: Int, threadNamePrefix: String) = {
    val counter = new AtomicInteger(0)
    val factory = new ThreadFactory {
      override def newThread(runnable: Runnable): Thread = {
        val thread = new Thread(runnable)
        thread.setName(s"$threadNamePrefix-${counter.incrementAndGet()}")
        thread.setDaemon(false)
        thread
      }
    }
    Executors.newFixedThreadPool(size, factory)
  }

  /**
   * Attempts graceful execution-context shutdown and logs if timeout is hit.
   *
   * @param ec
   *   Execution context backed by an executor service.
   * @param timeoutMinutes
   *   Maximum wait time for termination in minutes.
   */
  private def shutdownExecutor(ec: ExecutionContextExecutorService, timeoutMinutes: Int): Unit = {
    ec.shutdown()
    if (!ec.awaitTermination(timeoutMinutes.toLong, TimeUnit.MINUTES)) {
      logger.warn("Executor did not terminate cleanly within configured timeout")
    }
  }
}
