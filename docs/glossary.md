# Engineering Glossary

A living dictionary of concepts, patterns, distributed systems principles, and architectural terms used across this platform.

---

### Architectural Patterns

#### Microservices Architecture
* **Definition:** An architectural style that structures an application as a collection of loosely coupled, independently deployable services organized around business capabilities.
* **Why we use it:** Allows independent scaling, isolated deployment lifecycles, and strict physical boundaries between domain boundaries.
* **SEE:** `ADR-001`

#### Database-per-Service Pattern
* **Definition:** A design pattern where each microservice owns its private database. Other services can only access this data via the service's public APIs (REST/gRPC) or asynchronously published events.
* **Why we use it:** Prevents tight coupling via shared database schemas and eliminates cross-service SQL joins that create hidden dependencies.
* **SEE:** `ADR-001`

#### Multi-Project Gradle Monorepo
* **Definition:** A single git repository containing multiple coordinated Gradle subprojects (`libs/common`, `services/user-service`, etc.) governed by a centralized build definition.
* **Why we use it:** Provides atomic commits across contracts and consumers while maintaining independent deployable artifacts.

#### Transactional Outbox Pattern
* **Definition:** A pattern where domain events are written to a database table within the *same* local database transaction as the business entity changes, guaranteeing that the database write and the event recording succeed or fail atomically.
* **Why we use it:** Avoids dual-write inconsistencies between a relational database and a message broker (Kafka).

#### Saga Pattern (Choreography vs. Orchestration)
* **Definition:** A sequence of local transactions across distributed services where each step updates data within a single service and triggers the next step via events. If a step fails, compensating transactions are executed in reverse to roll back state.
* **Why we use it:** Manages distributed consistency across `order`, `payment`, and `dispatch` without distributed locking (2PC).

---

### Concurrency & Runtime

#### Virtual Threads (Project Loom - Java 21)
* **Definition:** Lightweight, JVM-managed threads designed to handle high-throughput, blocking I/O workloads. Unlike OS threads (which cost ~1MB of memory and require expensive kernel context switches), virtual threads cost hundreds of bytes and unmount from carrier threads during blocking calls.
* **Why we use it:** Allows writing simple, synchronous, imperative code (`thread-per-request`) that scales to hundreds of thousands of concurrent I/O operations without reactive framework complexity (Reactor/RxJava).
* **SEE:** `ADR-002`

#### Carrier Thread
* **Definition:** The underlying OS platform thread on which a virtual thread is mounted while executing CPU instructions.

---

### Reliability & Infrastructure

#### Testcontainers
* **Definition:** A Java library that provisions real, disposable Docker containers (PostgreSQL, Kafka, Redis) directly from JUnit 5 tests.
* **Why we use it:** Replaces flawed in-memory mocks (H2, EmbeddedKafka) with genuine production-grade engines, ensuring queries, isolation levels, and driver quirks match production exactly.

#### Flyway
* **Definition:** An open-source database migration tool that tracks, versions, and applies SQL schema migrations deterministically across environments.
* **Why we use it:** Schema as code. Guarantees that production, staging, and local databases evolve with an audit trail and repeatable state.
