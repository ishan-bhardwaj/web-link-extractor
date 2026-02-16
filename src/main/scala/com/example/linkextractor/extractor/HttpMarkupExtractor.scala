package com.example.linkextractor.extractor

import java.net.URI
import java.net.http.{HttpClient, HttpRequest, HttpResponse}
import java.nio.charset.StandardCharsets
import java.time.Duration
import scala.concurrent.{ExecutionContext, Future}
import scala.jdk.FutureConverters.CompletionStageOps
import scala.util.control.NonFatal

/**
 * HTTP implementation of markup extraction.
 *
 * @param config
 *   Extractor runtime configuration.
 * @param client
 *   HTTP client implementation.
 * @param ec
 *   Execution context.
 */
final class HttpMarkupExtractor(
  config: HttpMarkupExtractorConfig,
  client: HttpClient
)(implicit ec: ExecutionContext)
    extends MarkupExtractor {

  /**
   * Fetches markup for one URL using async HTTP GET.
   *
   * @param url
   *   URL to fetch.
   * @return
   *   Future with response body for successful 2xx statuses.
   */
  override def extract(url: String): Future[String] =
    try {
      val request = HttpRequest
        .newBuilder(URI.create(url))
        .timeout(Duration.ofMillis(config.requestTimeoutMs.toLong))
        .header("User-Agent", config.userAgent)
        .GET()
        .build()

      client
        .sendAsync(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))
        .asScala
        .flatMap { response =>
          val status = response.statusCode()
          if (status / 100 == 2) Future.successful(response.body())
          else Future.failed(new RuntimeException(s"HTTP $status for $url"))
        }
    } catch {
      case NonFatal(error) =>
        Future.failed(error)
    }
}

object HttpMarkupExtractor {

  /**
   * Creates an extractor using default project configuration.
   *
   * @param ec
   *   Execution context.
   * @return
   *   HTTP markup extractor instance.
   */
  def apply()(implicit ec: ExecutionContext): HttpMarkupExtractor =
    fromConfig(HttpMarkupExtractorConfig.default)

  /**
   * Creates an extractor from explicit configuration.
   *
   * @param config
   *   HTTP extractor configuration.
   * @param ec
   *   Execution context.
   * @return
   *   HTTP markup extractor instance.
   */
  def fromConfig(config: HttpMarkupExtractorConfig)(implicit ec: ExecutionContext): HttpMarkupExtractor =
    new HttpMarkupExtractor(
      config = config,
      client = defaultClient(config)
    )

  /**
   * Creates an extractor with an explicit HTTP client.
   *
   * @param config
   *   HTTP extractor configuration.
   * @param client
   *   HTTP client implementation.
   * @param ec
   *   Execution context.
   * @return
   *   HTTP markup extractor instance.
   */
  def withClient(
    config: HttpMarkupExtractorConfig,
    client: HttpClient
  )(implicit ec: ExecutionContext): HttpMarkupExtractor =
    new HttpMarkupExtractor(config = config, client = client)

  /**
   * Builds the default Java HTTP client.
   *
   * @param config
   *   HTTP extractor configuration.
   * @return
   *   Configured Java HTTP client.
   */
  private def defaultClient(config: HttpMarkupExtractorConfig): HttpClient =
    HttpClient
      .newBuilder()
      .followRedirects(HttpClient.Redirect.NORMAL)
      .connectTimeout(Duration.ofMillis(config.connectTimeoutMs.toLong))
      .build()
}
