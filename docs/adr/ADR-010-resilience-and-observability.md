# ADR-010: Resilience, Circuit Breakers & Distributed Observability

## Status
Accepted

## Context
In a distributed microservice ecosystem, network latency, ephemeral failures, and downstream dependency degradation can rapidly trigger cascading failures across the entire architecture.
For example, if `catalog-service` experiences elevated latency or connection pool exhaustion, incoming requests to `order-service` will block threads, saturate connection pools, and eventually crash `order-service` as well.
Simultaneously, debugging complex asynchronous workflows (such as Sagas spanning Kafka and WebSocket streams) without distributed correlation and metric instrumentation leads to prolonged Mean Time To Resolution (MTTR).

## Decisions

### 1. Resilience4j Circuit Breakers & Fallback Handlers
- **Decision:** All cross-service remote calls are shielded with Resilience4j `@CircuitBreaker` and `@Retry` annotations.
- **Sliding Window:** Count-based sliding window of 5 calls, with a failure rate threshold of 50%.
- **State Transition:** If 3 out of 5 calls fail, the circuit transitions from `CLOSED` to `OPEN`, immediately short-circuiting subsequent calls to execute local fallback methods without holding threads.
- **Graceful Degradation:** Fallbacks return safe default data (e.g. cached state) and increment the `swiggy.resilience.circuit.trips` metric counter for alert notifications.

### 2. Distributed Tracing with OpenTelemetry (W3C TraceContext)
- **Decision:** Utilize Micrometer Tracing with OpenTelemetry bridge (`micrometer-tracing-bridge-otel`).
- **Context Propagation:** W3C TraceContext standard headers (`traceparent`, `tracestate`) automatically propagate between services and Kafka messages.
- **MDC Logging:** All log entries are configured to output `[traceId=%X{traceId} spanId=%X{spanId} correlationId=%X{correlationId}]`, allowing operators to correlate logs across all services for any customer transaction in Grafana/Loki.

### 3. Metric Instrumentation & Prometheus
- **Decision:** Spring Boot Actuator and Micrometer are integrated to expose health, info, and metric endpoints.
- **Platform Metrics:**
  - `swiggy.orders.created.total`: Counter tagged by order status.
  - `swiggy.order.processing.duration`: Distribution timer tracking percentile latency.
  - `swiggy.resilience.circuit.trips`: Counter tracking downstream dependency trips.

## Consequences
- **Positive:** Protection against cascading system failures.
- **Positive:** Sub-second root cause diagnosis via unified `traceId` search across all logs.
- **Negative:** Requires careful tuning of circuit breaker sliding window thresholds to prevent false-positive trips during brief network blips.
