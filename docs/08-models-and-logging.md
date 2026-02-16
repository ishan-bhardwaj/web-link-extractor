# Models and Logging

## Files

- `src/main/scala/com/example/linkextractor/models/UrlMarkupRecord.scala`
- `src/main/scala/com/example/linkextractor/models/UrlLinksRecord.scala`
- `src/main/scala/com/example/linkextractor/utils/Logging.scala`

## Producer Queue Model: `UrlMarkupRecord`

Used between producer and consumer.

Variants:

- `MarkupSuccess(url, markup)`
- `MarkupFailed(url, errorMessage)`

Helpers:

- `UrlMarkupRecord.success(...)`
- `UrlMarkupRecord.failed(...)`

## Consumer Output Model: `UrlLinksRecord`

Used at consumer output sink boundary.

Variants:

- `Extracted(url, links)`
- `UpstreamFailed(url, errorMessage)`
- `ParseFailed(url, errorMessage)`

Helpers:

- `UrlLinksRecord.success(...)`
- `UrlLinksRecord.failedUpstream(...)`
- `UrlLinksRecord.failedParse(...)`

## Logging

`Logging` trait provides:

- `protected lazy val logger: Logger`

Any class/object can mix it in for standard SLF4J logging without repeating logger construction code.

