# Testing

## Test Scope

The suite includes unit and integration tests across all modules.

## Test Support

- `src/test/scala/com/example/linkextractor/TestSupport.scala`
  - `await(...)`
  - `drainQueue(...)`

## Queue Tests

- `queue/LockFreeBoundedQueueSpec.scala`
  - FIFO behavior
  - overflow drop-oldest
  - waiter behavior
  - close semantics
  - idempotent close
- `queue/QueueConfigSpec.scala`
  - config loading and validation

## Producer Tests

- `producer/UrlProducerSpec.scala`
  - success and failure mapping
  - synchronous throw handling
  - enqueue result handling
- `producer/UrlProducerPoolSpec.scala`
  - full URL processing over shared source
  - source/queue closure behavior on success and failure

## Consumer Tests

- `consumer/UrlConsumerSpec.scala`
  - success mapping
  - parse-failure mapping
  - upstream-failure mapping
  - sink failure suppression
- `consumer/ConsumerPoolSpec.scala`
  - full consumption lifecycle
  - shutdown hook invocation on success/failure

## Extractor Tests

- `extractor/RegexHyperlinkExtractorSpec.scala`
  - quoted/unquoted link extraction
- `extractor/HttpMarkupExtractorConfigSpec.scala`
  - config loading and validation
- `extractor/HttpMarkupExtractorSpec.scala`
  - success, non-2xx, async failure, invalid URL using stub client

## IO and Models

- `io/SourceSpec.scala`
  - normalization and file loading
- `io/SinkSpec.scala`
  - sink emission path
- `models/ModelSpec.scala`
  - smart constructor behavior

## Integration Tests

- `WebLinkExtractorIntegrationSpec.scala`
  - end-to-end producer + queue + consumer run
  - file-based `produceFromFile`
  - config-based pipeline run
  - full `runApplication` lifecycle

## Running Tests

```bash
sbt test
```

