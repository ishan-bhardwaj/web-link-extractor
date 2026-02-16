package com.example.linkextractor.extractor

import com.example.linkextractor.TestSupport
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

import java.net.http.HttpClient.Version
import java.net.http._
import java.net.{Authenticator, CookieHandler, ProxySelector, URI}
import java.time.Duration
import java.util
import java.util.Optional
import java.util.concurrent.{CompletableFuture, Executor}
import javax.net.ssl.{SSLContext, SSLParameters, SSLSession}
import scala.concurrent.ExecutionContext.Implicits.global

class HttpMarkupExtractorSpec extends AnyFunSuite with Matchers with TestSupport {

  private val config = HttpMarkupExtractorConfig(
    connectTimeoutMs = 1000,
    requestTimeoutMs = 1000,
    userAgent = "test-agent"
  )

  test("extract returns response body for 2xx status") {
    val response  = StubHttpResponse(status = 200, bodyValue = "<html>ok</html>")
    val client    = new StubHttpClient(Right(response))
    val extractor = HttpMarkupExtractor.withClient(
      config = config,
      client = client
    )

    await(extractor.extract("https://example.com")) shouldBe "<html>ok</html>"
  }

  test("extract fails for non-2xx status") {
    val response  = StubHttpResponse(status = 404, bodyValue = "not found")
    val client    = new StubHttpClient(Right(response))
    val extractor = HttpMarkupExtractor.withClient(
      config = config,
      client = client
    )

    val failure = intercept[RuntimeException] {
      await(extractor.extract("https://example.com"))
    }
    failure.getMessage should include("HTTP 404")
  }

  test("extract propagates async client failures") {
    val client    = new StubHttpClient(Left(new RuntimeException("network down")))
    val extractor = HttpMarkupExtractor.withClient(
      config = config,
      client = client
    )

    val failure = intercept[RuntimeException] {
      await(extractor.extract("https://example.com"))
    }
    failure.getMessage should include("network down")
  }

  test("extract fails fast for invalid URL syntax") {
    val client    = new StubHttpClient(Right(StubHttpResponse(status = 200, bodyValue = "ok")))
    val extractor = HttpMarkupExtractor.withClient(
      config = config,
      client = client
    )

    intercept[IllegalArgumentException] {
      await(extractor.extract("://bad-url"))
    }
  }

  private final case class StubHttpResponse(status: Int, bodyValue: String) extends HttpResponse[String] {
    private val _headers = HttpHeaders.of(
      util.Collections.emptyMap[String, util.List[String]](),
      (_: String, _: String) => true
    )

    override def statusCode(): Int = status

    override def request(): HttpRequest =
      HttpRequest.newBuilder(URI.create("https://example.com")).GET().build()

    override def previousResponse(): Optional[HttpResponse[String]] = Optional.empty()

    override def headers(): HttpHeaders = _headers

    override def body(): String = bodyValue

    override def sslSession(): Optional[SSLSession] = Optional.empty()

    override def uri(): URI = URI.create("https://example.com")

    override def version(): Version = Version.HTTP_1_1
  }

  private final class StubHttpClient(result: Either[Throwable, HttpResponse[String]]) extends HttpClient {

    override def cookieHandler(): Optional[CookieHandler] = Optional.empty()

    override def connectTimeout(): Optional[Duration] = Optional.of(Duration.ofMillis(1000))

    override def followRedirects(): HttpClient.Redirect = HttpClient.Redirect.NORMAL

    override def proxy(): Optional[ProxySelector] = Optional.empty()

    override def sslContext(): SSLContext = SSLContext.getDefault

    override def sslParameters(): SSLParameters = new SSLParameters()

    override def authenticator(): Optional[Authenticator] = Optional.empty()

    override def version(): Version = Version.HTTP_1_1

    override def executor(): Optional[Executor] = Optional.empty()

    override def newWebSocketBuilder(): WebSocket.Builder =
      throw new UnsupportedOperationException("Not used in tests")

    override def send[T](
      request: HttpRequest,
      responseBodyHandler: HttpResponse.BodyHandler[T]
    ): HttpResponse[T] = throw new UnsupportedOperationException("Not used in tests")

    override def sendAsync[T](
      request: HttpRequest,
      responseBodyHandler: HttpResponse.BodyHandler[T]
    ): CompletableFuture[HttpResponse[T]] =
      complete(result)

    override def sendAsync[T](
      request: HttpRequest,
      responseBodyHandler: HttpResponse.BodyHandler[T],
      pushPromiseHandler: HttpResponse.PushPromiseHandler[T]
    ): CompletableFuture[HttpResponse[T]] =
      complete(result)

    private def complete[T](result: Either[Throwable, HttpResponse[String]]): CompletableFuture[HttpResponse[T]] =
      result match {
        case Right(response) =>
          CompletableFuture.completedFuture(response.asInstanceOf[HttpResponse[T]])
        case Left(error) =>
          val failed = new CompletableFuture[HttpResponse[T]]()
          failed.completeExceptionally(error)
          failed
      }
  }
}
