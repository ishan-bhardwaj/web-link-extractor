package com.example.linkextractor.extractor

import com.typesafe.config.{Config, ConfigFactory}

/**
 * Runtime configuration for [[HttpMarkupExtractor]].
 *
 * @param connectTimeoutMs
 *   HTTP connect timeout in milliseconds.
 * @param requestTimeoutMs
 *   HTTP request timeout in milliseconds.
 * @param userAgent
 *   HTTP user-agent header.
 */
final case class HttpMarkupExtractorConfig(
  connectTimeoutMs: Int,
  requestTimeoutMs: Int,
  userAgent: String
) {
  require(connectTimeoutMs > 0, "connectTimeoutMs must be greater than 0")
  require(requestTimeoutMs > 0, "requestTimeoutMs must be greater than 0")
  require(userAgent.trim.nonEmpty, "userAgent must be non-empty")
}

object HttpMarkupExtractorConfig {
  private val SectionPath = "extractor.http"

  /**
   * Loads configuration from a Typesafe Config instance.
   *
   * @param config
   *   Source configuration.
   * @return
   *   Validated HTTP extractor configuration.
   */
  def load(config: Config): HttpMarkupExtractorConfig = {
    val section =
      if (config.hasPath(SectionPath)) config.getConfig(SectionPath)
      else throw new IllegalArgumentException(s"Missing config section: $SectionPath")

    HttpMarkupExtractorConfig(
      connectTimeoutMs = section.getInt("connect-timeout-ms"),
      requestTimeoutMs = section.getInt("request-timeout-ms"),
      userAgent = section.getString("user-agent")
    )
  }

  /**
   * Loads configuration from `application.conf`.
   *
   * @return
   *   Validated HTTP extractor configuration.
   */
  def load(): HttpMarkupExtractorConfig =
    load(ConfigFactory.load())

  /**
   * Lazily cached default configuration.
   *
   * @return
   *   Validated HTTP extractor configuration.
   */
  lazy val default: HttpMarkupExtractorConfig = load()
}
