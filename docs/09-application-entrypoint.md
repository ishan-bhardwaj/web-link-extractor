# Application Entrypoint and Wiring

## File

- `src/main/scala/com/example/linkextractor/WebLinkExtractor.scala`

## Public Methods

- `produceFromFile(filePath: String, ...)`
- `produceFromFile(path: Path, ...)`
- `runFilePipelineFromConfig(config: Config)`
- `runFilePipeline()`
- `runApplication(config: Config)`
- `main(args: Array[String])`

## Pipeline Wiring (`runFilePipelineFromConfig`)

1. Read counts and input path from `example` config block.
2. Build `Source` from file.
3. Build bounded queue from `queue.bounded.capacity`.
4. Create shared extractor from `extractor.http`.
5. Create producer pool with N workers.
6. Create consumer pool with M workers and sink shutdown hook.
7. Run producer and consumer futures concurrently (`zip`).

## Thread Pool (`runApplication`)

- Uses custom fixed-size executor with named threads.
- Converts executor to `ExecutionContextExecutorService`.
- Runs full pipeline with configured timeout.
- Performs graceful shutdown and timeout warning.

## Failure Handling

- `produceFromFile` and `runFilePipelineFromConfig` convert synchronous construction errors to failed futures.
- `runApplication` is synchronous and will throw if pipeline fails.

