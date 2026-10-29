# ADR-002: Build Tool (Gradle Kotlin DSL) and Runtime Selection (Java 21 Virtual Threads)

## Status
Accepted

## Date
2026-10-29

## Context
We need a modern, maintainable build automation tool and runtime for our high-throughput backend. The food delivery domain is dominated by I/O-bound operations (database queries, Redis lookups, Kafka event publishing, external payment gateway webhooks). We need high concurrent throughput without the cognitive overhead and debugging friction of reactive programming (e.g. Spring WebFlux / Project Reactor).

## Options Considered

### Build Tool
1. **Maven (`pom.xml`):** Traditional XML, declarative, standard, but more verbose and slower on incremental multi-module tasks.
2. **Gradle with Groovy DSL:** Flexible, but dynamically typed; IDE autocompletion and refactoring can be error-prone.
3. **Gradle with Kotlin DSL (`build.gradle.kts`):** Type-safe, compile-time checked build scripts, excellent IDE support, high incremental build performance.

### Java Runtime & Concurrency Model
1. **Java 17 with Platform Threads (Thread-per-request):** Limited by OS thread exhaustion (~200–500 threads per instance before heavy context switching overhead and OutOfMemory errors).
2. **Java 17 with Reactive (WebFlux / Reactor):** Non-blocking event loops. Scales high throughput, but complex stack traces, difficult debugging, viral `Mono`/`Flux` signatures, and incompatibility with standard thread-local contexts (e.g., standard JDBC/JPA without R2DBC).
3. **Java 21 with Virtual Threads (Project Loom):** Lightweight JVM-managed threads. Threads unmount during blocking I/O (JDBC, HTTP calls), yielding carrier threads. Retains synchronous, imperative, clean code while handling 10,000+ concurrent requests.

## Decision
- **Build Tool:** **Gradle with Kotlin DSL (`build.gradle.kts`)**. Gives us type-safe build scripts, clear plugin configuration, and high build speed.
- **Runtime:** **Java 21 LTS** with Virtual Threads enabled via Spring Boot 3 (`spring.threads.virtual.enabled=true`).

## Consequences

### Positive
- Build scripts are type-checked with instant IDE auto-completion.
- Simple synchronous programming model for all database queries and HTTP endpoints without losing concurrency.
- Elimination of reactive callback hell while matching or exceeding reactive throughput for I/O workloads.

### Negative / Trade-offs
- Virtual threads must avoid long CPU-intensive operations on the main thread (carrier thread pinning was improved in Java 21/24, but `synchronized` blocks inside blocking calls must still be monitored or replaced with `ReentrantLock`).
- Slightly higher memory usage than raw Netty event loops when holding large thread-local allocations (we will use Scoped Values or minimize ThreadLocal usage).

## Code and Config Affected
- `build.gradle.kts` (`java.sourceCompatibility = JavaVersion.VERSION_21`)
- `settings.gradle.kts`
- `src/main/resources/application.yml` (`spring.threads.virtual.enabled: true`)
