# Consumer Side

## Files

- `src/main/scala/com/example/linkextractor/consumer/Consumer.scala`
- `src/main/scala/com/example/linkextractor/consumer/UrlConsumer.scala`
- `src/main/scala/com/example/linkextractor/consumer/ConsumerPool.scala`

## `Consumer[-A]` Contract

- `consume(record: A): Future[Unit]`

The consumer handles one record only; queue polling and worker lifecycle are delegated to `ConsumerPool`.

## `UrlConsumer`

Input:

- `UrlMarkupRecord` from producer queue

Dependencies:

- hyperlink extraction function `String => Vector[String]`
- output sink `Sink`

Mapping:

- `MarkupSuccess(url, markup)`:
  - parse links
  - emit `UrlLinksRecord.Extracted`
  - parse failure -> `UrlLinksRecord.ParseFailed`
- `MarkupFailed(url, errorMessage)`:
  - emit `UrlLinksRecord.UpstreamFailed`

Error isolation:

- sink failure for one record is logged and suppressed
- subsequent records continue

## `ConsumerPool[A]`

Parameters:

- `consumerCount`
- `consumerFactory: () => Consumer[A]`
- optional `onShutdown` hook (e.g., `sink.close`)

Lifecycle:

1. Build worker tasks.
2. Start workers concurrently.
3. Worker loop:
   - `dequeue()`
   - `Some(record)` -> `consume(record)` -> recurse
   - `None` -> terminate worker
4. On completion/failure:
   - run shutdown hook (best effort)

Termination rule:

- Queue returning `None` signals closed-and-drained condition.

