# Configuration

## Main Config File

- `src/main/resources/application.conf`

## Keys

### Queue

- `queue.bounded.capacity`
  - max in-memory buffered records before drop-oldest policy applies

### HTTP Extractor

- `extractor.http.connect-timeout-ms`
- `extractor.http.request-timeout-ms`
- `extractor.http.user-agent`

### Example Pipeline

- `example.input-file-path`
- `example.producer-count`
- `example.consumer-count`

### Runtime

- `runtime.thread-pool-size`
- `runtime.await-timeout-minutes`

## Config Load Points

- Queue config:
  - `QueueConfig.BoundedQueueConfig.load(config)`
- HTTP extractor config:
  - `HttpMarkupExtractorConfig.load(config)`
- Application runtime:
  - read directly in `WebLinkExtractor.runApplication`

