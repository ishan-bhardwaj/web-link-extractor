# API Reference (File by File)

This section maps every source/test file to its purpose and key API.

## Main Application

- `src/main/scala/com/example/linkextractor/WebLinkExtractor.scala`
  - app orchestration
  - producer-only helpers
  - full pipeline helpers
  - `runApplication` and `main`

## Queue Package

- `src/main/scala/com/example/linkextractor/queue/Queue.scala`
  - queue abstraction
  - enqueue result domain
  - queue factories
- `src/main/scala/com/example/linkextractor/queue/QueueConfig.scala`
  - bounded queue config model + loaders
- `src/main/scala/com/example/linkextractor/queue/LockFreeBoundedQueue.scala`
  - lock-free bounded implementation with CAS transitions

## IO Package

- `src/main/scala/com/example/linkextractor/io/Source.scala`
  - source contract
  - in-memory and file-backed sources
  - normalization rules
- `src/main/scala/com/example/linkextractor/io/Sink.scala`
  - sink contract
  - console sink implementation

## Extractor Package

- `src/main/scala/com/example/linkextractor/extractor/MarkupExtractor.scala`
  - markup extractor contract
- `src/main/scala/com/example/linkextractor/extractor/HttpMarkupExtractor.scala`
  - HTTP markup extractor implementation + factories
- `src/main/scala/com/example/linkextractor/extractor/HttpMarkupExtractorConfig.scala`
  - extractor config model + loader
- `src/main/scala/com/example/linkextractor/extractor/HyperlinkExtractor.scala`
  - hyperlink extractor contract
- `src/main/scala/com/example/linkextractor/extractor/RegexHyperlinkExtractor.scala`
  - regex hyperlink extraction implementation

## Producer Package

- `src/main/scala/com/example/linkextractor/producer/Producer.scala`
  - producer contract
- `src/main/scala/com/example/linkextractor/producer/UrlProducer.scala`
  - URL fetch + record conversion + enqueue handling
- `src/main/scala/com/example/linkextractor/producer/UrlProducerPool.scala`
  - concurrent URL worker orchestration

## Consumer Package

- `src/main/scala/com/example/linkextractor/consumer/Consumer.scala`
  - consumer contract
- `src/main/scala/com/example/linkextractor/consumer/UrlConsumer.scala`
  - markup-to-link record conversion + sink emission
- `src/main/scala/com/example/linkextractor/consumer/ConsumerPool.scala`
  - concurrent queue-consumer worker orchestration

## Models and Utilities

- `src/main/scala/com/example/linkextractor/models/UrlMarkupRecord.scala`
  - producer output record type
- `src/main/scala/com/example/linkextractor/models/UrlLinksRecord.scala`
  - consumer output record type
- `src/main/scala/com/example/linkextractor/utils/Logging.scala`
  - shared SLF4J logger trait

## Resources

- `src/main/resources/application.conf`
  - runtime config
- `src/main/resources/urls.txt`
  - sample input URL list

## Tests

- `src/test/scala/com/example/linkextractor/TestSupport.scala`
- `src/test/scala/com/example/linkextractor/WebLinkExtractorIntegrationSpec.scala`
- `src/test/scala/com/example/linkextractor/consumer/ConsumerPoolSpec.scala`
- `src/test/scala/com/example/linkextractor/consumer/UrlConsumerSpec.scala`
- `src/test/scala/com/example/linkextractor/extractor/HttpMarkupExtractorConfigSpec.scala`
- `src/test/scala/com/example/linkextractor/extractor/HttpMarkupExtractorSpec.scala`
- `src/test/scala/com/example/linkextractor/extractor/RegexHyperlinkExtractorSpec.scala`
- `src/test/scala/com/example/linkextractor/io/SinkSpec.scala`
- `src/test/scala/com/example/linkextractor/io/SourceSpec.scala`
- `src/test/scala/com/example/linkextractor/models/ModelSpec.scala`
- `src/test/scala/com/example/linkextractor/producer/UrlProducerPoolSpec.scala`
- `src/test/scala/com/example/linkextractor/producer/UrlProducerSpec.scala`
- `src/test/scala/com/example/linkextractor/queue/LockFreeBoundedQueueSpec.scala`
- `src/test/scala/com/example/linkextractor/queue/QueueConfigSpec.scala`

