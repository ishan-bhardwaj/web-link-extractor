# Enhancements

This project is production-leaning and intentionally simple, but it can be improved further in several areas.

## Intentional Design Choice: Custom Queue

The lock-free bounded queue in this project was implemented intentionally.

Reasons:

- The assignment explicitly emphasized queue behavior and concurrency.
- Building a queue in-project demonstrates algorithmic reasoning, CAS-based state management, and shutdown semantics.
- It keeps the core pipeline dependency-light and easy to inspect end to end.

For many real production systems, using a battle-tested queue implementation (like Chronicle Queue, or even Kafka) is still the better default.

## 1. Stronger HTML Parsing

Current state:

- Hyperlink extraction uses regex (`RegexHyperlinkExtractor`).

Potential improvement:

- Add a parser-backed extractor (e.g., JSoup) for better HTML correctness.
- Keep regex as a lightweight fallback strategy.

Benefits:

- Better handling of malformed HTML.
- More accurate link extraction across edge cases.

## 2. Backpressure and Queue Strategy Options

Current state:

- Bounded queue uses drop-oldest on overflow.
- Queue is custom in-project implementation.

Potential improvement:

- Add pluggable overflow policies:
  - drop-oldest (current)
  - drop-newest
  - reject
  - block with timeout
- Expose policy selection in config.

Benefits:

- Better control over data-loss behavior per workload.

## 3. Alternative Queue Implementations (Production Option)

If requirements move beyond this exercise-oriented implementation, consider replacing the custom queue with a specialized library.

Examples:

- Chronicle Queue
  - strong option when you need persisted/off-heap queues and replayability.
- JCTools MPSC/MPMC queues
  - high-throughput in-memory lock-free queues.
- LMAX Disruptor
  - very low-latency event processing patterns.
- Akka Streams / fs2 queues
  - higher-level backpressure and stream semantics.

Selection criteria:

- in-memory vs durable queue requirements
- single-node vs distributed architecture
- latency/throughput targets
- operational complexity and observability needs

## 4. Producer/Consumer Metrics and Tracing

Current state:

- Logging is available, but no structured metrics.

Potential improvement:

- Add metrics for:
  - enqueue/dequeue rates
  - overflow drops
  - fetch latency
  - parse latency
  - success/failure counts
- Add OpenTelemetry tracing around producer/consumer stages.

Benefits:

- Faster production diagnosis and capacity planning.

## 5. Retry and Circuit-Breaking for HTTP

Current state:

- One-shot HTTP fetch with timeout.

Potential improvement:

- Add configurable retry policy with exponential backoff and jitter.
- Add per-host circuit breaker to avoid cascading failures.

Benefits:

- Better resilience under transient network/service failures.

## 6. Output Targets Beyond Console

Current state:

- `Sink.console()` only.

Potential improvement:

- Add sinks for:
  - JSONL file
  - CSV file
  - Kafka topic
  - DB table
- Add fan-out sink composition.

Benefits:

- Easier integration into downstream systems.

## 7. Graceful Stop and Lifecycle Hooks

Current state:

- Queue close is producer-pool driven.

Potential improvement:

- Add explicit application-level shutdown API:
  - stop producers
  - drain queue
  - stop consumers
- Add JVM shutdown hook integration.

Benefits:

- Cleaner controlled shutdown in managed environments.

## 8. Scale-Oriented Source Strategy

Current state:

- Source is iterator-based and synchronized for multi-worker access.

Potential improvement:

- Add chunked/batched source adapters with prefetch buffering.
- Add optional asynchronous source loading for very large input files.

Benefits:

- Better throughput for huge URL lists.

## 9. Configuration Hardening

Current state:

- Config is validated at case-class constructor level.

Potential improvement:

- Add startup-time full config validation report.
- Add environment-specific config overlays and schema docs.

Benefits:

- Faster failure detection and safer deployments.

## 10. More Deterministic Concurrency Tests

Current state:

- Tests cover behavior thoroughly but mostly with runtime scheduling.

Potential improvement:

- Add deterministic concurrency/property tests:
  - queue linearizability checks
  - repeated randomized producer/consumer schedules

Benefits:

- Higher confidence in lock-free correctness under stress.

## 11. Packaging and Delivery

Current state:

- Standard sbt project.

Potential improvement:

- Add:
  - assembly packaging
  - Docker image
  - CI pipeline (format, compile, test, coverage gates)
  - release tagging/versioning workflow

Benefits:

- Easier distribution and operational adoption.
