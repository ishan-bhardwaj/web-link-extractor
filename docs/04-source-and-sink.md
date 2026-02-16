# Source and Sink

## Files

- `src/main/scala/com/example/linkextractor/io/Source.scala`
- `src/main/scala/com/example/linkextractor/io/Sink.scala`

## `Source`

Purpose:

- Provide one-pass URL iteration independent of producer logic.

API:

- `stream: Iterator[String]`
- `close(): Unit`

Factories:

- `Source.fromInputs(inputs: Iterable[String])`
- `Source.fromFile(path: String | Path[, charset])`

Normalization rules:

- trim whitespace
- drop blank lines
- drop comment lines that start with `#`

Resource handling:

- file-backed source closes underlying `scala.io.Source`
- in-memory source close is no-op

## `Sink`

Purpose:

- Receive consumer output records.

API:

- `emit(result: UrlLinksRecord): Future[Unit]`
- `close(): Unit`

Provided implementation:

- `Sink.console()`
  - logs:
    - extracted links
    - upstream fetch failures
    - parse failures

