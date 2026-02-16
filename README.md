# Web Link Extractor

A simple, concurrent producer/consumer web link extractor built in Scala.

The project reads URLs from a source, fetches markup concurrently, pushes results to a lock-free bounded queue, consumes those results concurrently, extracts hyperlinks, and writes output through a sink.

## Documentation

Detailed documentation lives in [`docs/`](docs/README.md).

## Requirements

- JDK 11+ (Java HTTP client is used)
- `sbt` installed and available on `PATH`

## Run

1. Update input/config as needed in [`src/main/resources/application.conf`](src/main/resources/application.conf).
2. Run the app:

```bash
sbt run
```

By default it reads URLs from [`src/main/resources/urls.txt`](src/main/resources/urls.txt) and runs with producer/consumer counts from config.

## Test

Run full unit + integration test suite:

```bash
sbt test
```

## High-Level Flow

1. `Source` yields URLs.
2. `UrlProducerPool` runs N producers concurrently.
3. Producers call markup extraction and enqueue `UrlMarkupRecord`.
4. `ConsumerPool` runs M consumers concurrently.
5. Consumers dequeue records, extract links, and emit `UrlLinksRecord` to `Sink`.
6. Queue close + drain defines completion.

