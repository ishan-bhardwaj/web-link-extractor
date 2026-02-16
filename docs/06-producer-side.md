# Producer Side

## Files

- `src/main/scala/com/example/linkextractor/producer/Producer.scala`
- `src/main/scala/com/example/linkextractor/producer/UrlProducer.scala`
- `src/main/scala/com/example/linkextractor/producer/UrlProducerPool.scala`

## `Producer[A]` Contract

- `produce(url: String, outputQueue: Queue[A]): Future[Unit]`

The producer processes one URL only; stream traversal belongs to pool orchestration.

## `UrlProducer`

Input:

- function `String => Future[String]` for markup fetch

Output:

- `UrlMarkupRecord` enqueued into queue

Behavior:

- success fetch -> `UrlMarkupRecord.MarkupSuccess`
- failed fetch -> `UrlMarkupRecord.MarkupFailed`
- synchronous throw from extractor -> converted to failed future and handled

Queue result handling (explicit):

- `Enqueued`: no-op
- `EnqueuedAfterDroppingOldest`: warn
- `Closed`: warn

## `UrlProducerPool`

Parameters:

- `producerCount`
- `producerFactory: () => Producer[UrlMarkupRecord]`

Lifecycle:

1. Get shared source iterator.
2. Build deferred worker tasks.
3. Start all worker futures concurrently.
4. For each worker:
   - claim next URL in synchronized block
   - call producer
   - recurse until stream exhausted
5. On completion/failure:
   - close source (best effort)
   - close output queue

Concurrency note:

- Source iterator access is synchronized because `Iterator` is not thread-safe.

