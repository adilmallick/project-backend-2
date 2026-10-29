# Platform Implementation & Learning Progress

A living audit and progress tracker across all architectural stages, milestones, ADRs, and verification suites.

---

## Stage Roadmap Status

| Stage | Name | Target Capabilities | Status | Branch | ADRs |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **Stage 0** | **Setup & Foundation** | Java 21, Gradle Monorepo, Docker Compose (PostgreSQL 16 Multi-DB), CI Pipeline, Testcontainers. | **DONE** ✅ | `main` | ADR-001, ADR-002 |
| **Stage 1** | **Core Domain & Security** | Microservices Domain Models, Flyway, JWT Auth & RBAC, N+1 Query Fix, Order State Machine, Optimistic Locking, OpenAPI. | **DONE** ✅ | `feat/stage-1-core-domain` | ADR-003, ADR-004, ADR-005 |
| **Stage 2** | **Redis Caching & State** | Menu Caching, Rate Limiting, Idempotency Keys, Redis GEO for Riders, Redis Outage Chaos Exercise. | **DONE** ✅ | `feat/stage-2-redis` | ADR-006, ADR-007 |
| **Stage 3** | **Kafka & Event-Driven Sagas** | Transactional Outbox, Debezium CDC, Avro + Schema Registry, Saga Compensation, Broker Chaos. | **DONE** ✅ | `feat/stage-3-kafka-sagas` | ADR-008 |
| **Stage 4** | **Live Tracking & WebSockets** | STOMP WebSockets, Redis Pub/Sub Backplane, Location broadcast, Reconnection. | **PENDING** ⏳ | `feat/stage-4-websockets` | - |
| **Stage 5** | **Production Hardening** | OpenTelemetry Tracing, Prometheus + Grafana, Resilience4j, k6 Load Tests, Container Security. | **PENDING** ⏳ | `feat/stage-5-hardening` | - |
| **Stage 6** | **Cloud Deployment (AWS)** | Terraform, ECS Fargate, RDS PostgreSQL, ElastiCache Redis, MSK Kafka, Budget Alerts. | **PENDING** ⏳ | `feat/stage-6-aws-terraform` | - |

---

## Detailed Stage Checklists

### Stage 0: Setup & Foundation (COMPLETED ✅)
- [x] Multi-project Gradle Monorepo layout (`settings.gradle.kts`, `build.gradle.kts`).
- [x] Java 21 LTS with Virtual Threads enabled (`spring.threads.virtual.enabled=true`).
- [x] Docker Compose multi-database PostgreSQL 16 container (`user_db`, `catalog_db`, `order_db`).
- [x] `libs/common` shared library: `ApiResponse<T>`, `ErrorDetails`, `GlobalExceptionHandler`, `CorrelationIdFilter`.
- [x] GitHub Actions CI workflow skeleton (`.github/workflows/ci.yml`).
- [x] Testcontainers integration testing configured with real PostgreSQL 16.
- [x] [ADR-001: Multi-Project Microservices Architecture from Day 1](file:///Users/adil/Desktop/project-backend-2/docs/adr/ADR-001-modular-monolith-architecture.md)
- [x] [ADR-002: Java 21 Virtual Threads & Gradle Kotlin DSL](file:///Users/adil/Desktop/project-backend-2/docs/adr/ADR-002-build-tool-and-runtime-selection.md)
- [x] Living glossary established: [`docs/glossary.md`](file:///Users/adil/Desktop/project-backend-2/docs/glossary.md).

---

### Stage 1: Core Domain & Security (COMPLETED ✅)
- [x] **Increment 1.1:** `user-service` Identity, BCrypt (cost factor 12), Stateless JWT Auth, RBAC (`CUSTOMER`, `RESTAURANT_OWNER`, `RIDER`, `ADMIN`), Testcontainers suite.
  - [x] [ADR-003: Stateless JWT Authentication with RBAC](file:///Users/adil/Desktop/project-backend-2/docs/adr/ADR-003-stateless-jwt-authentication.md)
  - [x] Summary doc: [`docs/stage-1-increment-1.1-auth.md`](file:///Users/adil/Desktop/project-backend-2/docs/stage-1-increment-1.1-auth.md)
- [x] **Increment 1.2:** `catalog-service` Restaurant & MenuItem entities, N+1 query disaster demonstration and `@EntityGraph` production fix, paginated search with `Pageable`.
  - [x] [ADR-004: JPA Query Optimization and N+1 Prevention](file:///Users/adil/Desktop/project-backend-2/docs/adr/ADR-004-jpa-query-optimization-and-n-plus-1-prevention.md)
  - [x] Summary doc: [`docs/stage-1-increment-1.2-catalog-nplus1.md`](file:///Users/adil/Desktop/project-backend-2/docs/stage-1-increment-1.2-catalog-nplus1.md)
- [x] **Increment 1.3:** `order-service` Order entity, OrderItem entity, Order State Machine (`ORDER_PLACED` -> `RESTAURANT_ACCEPTED` -> `RIDER_ASSIGNED` -> `PICKED_UP` -> `DELIVERED`), point-in-time pricing snapshot, optimistic locking (`@Version`).
  - [x] [ADR-005: Order State Machine & Optimistic Locking Concurrency](file:///Users/adil/Desktop/project-backend-2/docs/adr/ADR-005-order-state-machine-concurrency.md)
- [x] **OpenAPI / Swagger UI Docs:** Interactive documentation enabled across all microservices (`/swagger-ui.html`).
- [x] **Chaos Exercise 1:** Concurrent Order Race Conditions (Lost Update prevention via JPA `@Version`).
- [x] Stage 1 Summary document: [`docs/stage-1-summary.md`](file:///Users/adil/Desktop/project-backend-2/docs/stage-1-summary.md).

---

### Stage 2: Redis Caching & Distributed State (COMPLETED ✅)
- [x] **Redis 7 Infrastructure:** Added `delivery-redis` container with health checks and persistent volume in `infra/docker/docker-compose.yml`.
- [x] **Restaurant Menu Caching:** `@Cacheable(value = "restaurants", key = "#id")` with 15-min TTL and Jackson JSON serialization (`catalog-service`).
- [x] **Cache Invalidation:** Instantaneous `@CacheEvict` on menu item creation / updates (`catalog-service`).
- [x] **Chaos Exercise 2 (Redis Failure Resilience):** Resilient `CacheErrorHandler` falling back gracefully to PostgreSQL when Redis is unreachable (`RedisOutageChaosIntegrationTests`).
- [x] **Sliding-Window Rate Limiting:** Redis `ZSET` log rate limiter rejecting brute-force requests with HTTP 429 and `Retry-After` header (`user-service`).
- [x] **Distributed Idempotency:** Redis `SETNX` preventing duplicate order placement and double-billing on network retry (`order-service`).
- [x] **Rider Geospatial Dispatching:** Dedicated `dispatch-service` microservice using Redis `GEOADD` and `GEOSEARCH` to locate nearest riders within radius.
- [x] [ADR-006: Redis Cache Eviction & Serialization Strategies](file:///Users/adil/Desktop/project-backend-2/docs/adr/ADR-006-redis-caching-and-invalidation.md)
- [x] [ADR-007: Distributed Rate Limiting & Idempotency using Redis](file:///Users/adil/Desktop/project-backend-2/docs/adr/ADR-007-distributed-rate-limiting-and-idempotency.md)
- [x] Stage 2 Summary document: [`docs/stage-2-summary.md`](file:///Users/adil/Desktop/project-backend-2/docs/stage-2-summary.md).

---

### Stage 3: Kafka & Event-Driven Sagas (COMPLETED ✅)
- [x] **Apache Kafka KRaft Infrastructure:** Configured `delivery-kafka` container with KRaft mode in `infra/docker/docker-compose.yml`.
- [x] **Transactional Outbox Pattern:** Flyway migration `V2__init_outbox_schema.sql` and `OutboxEvent` entity staged atomically with `Order` within `@Transactional`.
- [x] **Outbox Publisher:** `OutboxPublisherService` streaming domain events to Kafka topic `order.events` partitioned by `orderId`.
- [x] **Event-Driven Saga Choreography:** `PaymentServiceSimulator` processing `OrderPlacedEvent` and publishing `PaymentEvent`.
- [x] **Saga Compensating Transactions:** `OrderSagaCoordinator` advancing order to `RESTAURANT_ACCEPTED` on payment success, or triggering compensating transaction to `CANCELLED` on payment decline.
- [x] **Chaos Exercise 3 (Broker Outage Resilience):** Verified order creation and outbox persistence during total Kafka broker downtime (`OrderOutboxChaosIntegrationTests`).
- [x] [ADR-008: Transactional Outbox Pattern & Event-Driven Sagas](file:///Users/adil/Desktop/project-backend-2/docs/adr/ADR-008-transactional-outbox-and-sagas.md)
- [x] Stage 3 Summary document: [`docs/stage-3-summary.md`](file:///Users/adil/Desktop/project-backend-2/docs/stage-3-summary.md).
