# System Overview

## Goal

Implement a simple producer/consumer pipeline for web link extraction:

1. Read URLs from inputs (file/direct values).
2. Fetch URL markup concurrently.
3. Push fetch results into a bounded queue.
4. Consume queue results concurrently.
5. Extract hyperlinks from markup.
6. Emit output per URL.

## Main Components

- Queue abstraction + lock-free bounded implementation:
  - `src/main/scala/com/example/linkextractor/queue/Queue.scala`
  - `src/main/scala/com/example/linkextractor/queue/LockFreeBoundedQueue.scala`
- IO boundaries:
  - `src/main/scala/com/example/linkextractor/io/Source.scala`
  - `src/main/scala/com/example/linkextractor/io/Sink.scala`
- Producer side:
  - `src/main/scala/com/example/linkextractor/producer/Producer.scala`
  - `src/main/scala/com/example/linkextractor/producer/UrlProducer.scala`
  - `src/main/scala/com/example/linkextractor/producer/UrlProducerPool.scala`
- Consumer side:
  - `src/main/scala/com/example/linkextractor/consumer/Consumer.scala`
  - `src/main/scala/com/example/linkextractor/consumer/UrlConsumer.scala`
  - `src/main/scala/com/example/linkextractor/consumer/ConsumerPool.scala`
- Extraction logic:
  - `src/main/scala/com/example/linkextractor/extractor/*.scala`
- Application wiring:
  - `src/main/scala/com/example/linkextractor/WebLinkExtractor.scala`

## Concurrency Model

- Producers and consumers run concurrently using `Future` workers on a shared `ExecutionContext`.
- URL source iteration is synchronized inside producer pool to ensure one URL is claimed once.
- Queue is lock-free (CAS + immutable state snapshots).
- Shutdown protocol:
  - Producer pool closes queue when source is exhausted or on fatal producer-pool failure.
  - Consumer workers terminate when queue reports `None` (closed and drained).

## Error Isolation Model

- Per-URL fetch failures are converted to `UrlMarkupRecord.MarkupFailed`.
- Per-URL parse failures are converted to `UrlLinksRecord.ParseFailed`.
- Sink emission failures are logged and suppressed per item.
- One bad URL does not terminate remaining URLs unless failure is at orchestration layer (e.g., producer worker future fails unexpectedly).

