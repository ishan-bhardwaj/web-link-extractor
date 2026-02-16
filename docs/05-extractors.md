# Extractors

## Files

- `src/main/scala/com/example/linkextractor/extractor/MarkupExtractor.scala`
- `src/main/scala/com/example/linkextractor/extractor/HttpMarkupExtractor.scala`
- `src/main/scala/com/example/linkextractor/extractor/HttpMarkupExtractorConfig.scala`
- `src/main/scala/com/example/linkextractor/extractor/HyperlinkExtractor.scala`
- `src/main/scala/com/example/linkextractor/extractor/RegexHyperlinkExtractor.scala`

## Markup Extraction

`MarkupExtractor` trait:

- `extract(url: String): Future[String]`

`HttpMarkupExtractor`:

- Uses Java `HttpClient` async API.
- Sends GET with:
  - configured request timeout
  - configured user-agent
- Treats only 2xx as success.
- Returns failed future for non-2xx status.
- Wraps synchronous errors (invalid URI, etc.) into failed futures.

## HTTP Config

`HttpMarkupExtractorConfig` fields:

- `connectTimeoutMs`
- `requestTimeoutMs`
- `userAgent`

Loaded from:

- `extractor.http.connect-timeout-ms`
- `extractor.http.request-timeout-ms`
- `extractor.http.user-agent`

## Hyperlink Extraction

`HyperlinkExtractor` trait:

- `extractLinks(markup: String): Vector[String]`

`RegexHyperlinkExtractor`:

- lightweight regex parser for `href=...` patterns
- supports:
  - double-quoted
  - single-quoted
  - unquoted attribute values
- emits links in match order

