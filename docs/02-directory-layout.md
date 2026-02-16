# Directory Layout

## Top Level

- `build.sbt`: Main sbt build definition.
- `project/Versions.scala`: Centralized project + dependency versions.
- `project/Dependencies.scala`: Centralized library dependency groups.
- `project/plugins.sbt`: Build plugins.
- `src/main/resources/application.conf`: Runtime configuration.
- `src/main/resources/urls.txt`: Example URL input file.
- `src/main/scala/...`: Production code.
- `src/test/scala/...`: Unit + integration tests.

## Main Code Packages

- `com.example.linkextractor`
  - `WebLinkExtractor.scala`: app orchestration and `main`.
- `...consumer`
  - Generic consumer contract and consumer worker pools.
- `...extractor`
  - Markup extraction (HTTP) and hyperlink extraction (regex).
- `...io`
  - Input `Source` and output `Sink`.
- `...models`
  - Pipeline domain records.
- `...producer`
  - Generic producer contract and URL producer pool.
- `...queue`
  - Queue interface/config and lock-free bounded queue.
- `...utils`
  - Shared logging trait.

## Test Packages

- Mirrors production structure for focused unit tests.
- Includes an end-to-end integration spec at:
  - `src/test/scala/com/example/linkextractor/WebLinkExtractorIntegrationSpec.scala`

