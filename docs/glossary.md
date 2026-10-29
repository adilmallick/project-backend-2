# Engineering Glossary

A living dictionary of concepts, patterns, distributed systems principles, and architectural terms used across this platform.

---

### Architectural & Security Patterns

#### Microservices Architecture
* **Definition:** An architectural style that structures an application as a collection of loosely coupled, independently deployable services organized around business capabilities.
* **Why we use it:** Allows independent scaling, isolated deployment lifecycles, and strict physical boundaries between domain boundaries.
* **SEE:** `ADR-001`

#### Database-per-Service Pattern
* **Definition:** A design pattern where each microservice owns its private database. Other services can only access this data via the service's public APIs (REST/gRPC) or asynchronously published events.
* **Why we use it:** Prevents tight coupling via shared database schemas and eliminates cross-service SQL joins that create hidden dependencies.
* **SEE:** `ADR-001`

#### Stateless Authentication (JWT)
* **Definition:** An authentication mechanism where the server signs a cryptographically verifiable token containing client identity claims and issues it to the client. Subsequent requests attach this token, and any service verifies it without querying a database or session store.
* **Why we use it:** Eliminates database lookups on every incoming HTTP request, ensuring high throughput across distributed instances.
* **SEE:** `ADR-003`

#### Role-Based Access Control (RBAC)
* **Definition:** A security access control model that restricts resource access based on the user's assigned business role (`CUSTOMER`, `RESTAURANT_OWNER`, `RIDER`, `ADMIN`).
* **Why we use it:** Enforces least-privilege access across microservice endpoints.

#### BCrypt Password Hashing with Salt
* **Definition:** An adaptive, cryptographic password-hashing function based on the Blowfish cipher that incorporates a random salt and a configurable work factor (cost) to protect against brute-force and rainbow table attacks.
* **Why we use it:** Guarantees that even if database records are compromised, stored passwords cannot be reversed or cracked via parallel GPU acceleration.

#### JPA N+1 Query Problem
* **Definition:** A performance anti-pattern where an ORM executes 1 query to fetch $N$ parent entities, followed by $N$ separate queries to fetch the lazy-loaded child collection for each parent entity, resulting in $1 + N$ total database round-trips.
* **Why we fix it:** Causes severe database connection pool exhaustion and latency spikes under concurrent load.
* **SEE:** `ADR-004`

#### `@EntityGraph` / `JOIN FETCH`
* **Definition:** A JPA optimization technique that instructs Hibernate to generate an SQL `LEFT OUTER JOIN` clause in a single query, eagerly fetching parent and child entities in one round-trip while keeping the mapping `FetchType.LAZY` by default.
* **Why we use it:** Collapses $1 + N$ queries into exactly $1$ query without the permanent memory bloat of `FetchType.EAGER`.
* **SEE:** `ADR-004`

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
