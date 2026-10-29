# Stage 0: Microservices Architecture & Platform Setup

**Date:** 2026-10-30  
**Status:** Complete  
**Branch:** `feat/stage-0-setup`  
**Runtime:** Java 21 LTS (Virtual Threads / Project Loom)  
**Build Tool:** Gradle 8.10.2 (Kotlin DSL)  

---

## 1. Architectural Blueprint

We established a **Multi-Project Microservices Monorepo** with a strict **Database-per-Service** boundary from Day 1.

```mermaid
graph TD
    Client[Client / Web / Mobile] --> Gateway[API Gateway / Port 8080]

    subgraph Core Microservices
        Gateway --> UserService[user-service<br/>Port 8081]
        Gateway --> CatalogService[catalog-service<br/>Port 8082]
        Gateway --> OrderService[order-service<br/>Port 8083]
    end

    subgraph Data Tier (Physical Isolation)
        UserService --> UserDB[(PostgreSQL user_db)]
        CatalogService --> CatalogDB[(PostgreSQL catalog_db)]
        OrderService --> OrderDB[(PostgreSQL order_db)]
    end

    subgraph Shared Kernel
        CommonLib[libs:common<br/>ApiResponse, ErrorDetails, CorrelationIdFilter]
        UserService -.-> CommonLib
        CatalogService -.-> CommonLib
        OrderService -.-> CommonLib
    end
```

---

## 2. Service Matrix

| Module | Type | Port | Database | Primary Responsibility | Key Entities / Schema |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **`libs:common`** | Shared Java Library | N/A | None | API envelope, correlation filter, error handling | `ApiResponse<T>`, `ErrorDetails`, `CorrelationIdFilter` |
| **`services:user-service`** | Spring Boot App | `8081` | `user_db` | User identity, authentication, roles (Customer, Rider, Restaurant) | `users` table, BCrypt hashes, UUID PK |
| **`services:catalog-service`** | Spring Boot App | `8082` | `catalog_db` | Restaurant profiles, menus, items, item pricing in integer cents | `restaurants`, `menu_items` tables |
| **`services:order-service`** | Spring Boot App | `8083` | `order_db` | Order lifecycle, saga state machine, line items, optimistic locking | `orders`, `order_items` tables, state constraints |

---

## 3. Core Architectural Decisions (ADR Summary)

### [ADR-001: Multi-Project Microservices Architecture from Day 1](file:///Users/adil/Desktop/project-backend-2/docs/adr/ADR-001-modular-monolith-architecture.md)
* **Decision:** Build independent Spring Boot microservices inside a Gradle monorepo with separate databases per service.
* **Why:** Enforces strict physical boundaries immediately. Prevents cross-service SQL joins and prepares each service for independent scaling and deployment (ECS Fargate in Stage 6).
* **Trade-off:** Multiple JVM processes running locally and multi-database provisioning overhead.

### [ADR-002: Java 21 Virtual Threads & Gradle Kotlin DSL](file:///Users/adil/Desktop/project-backend-2/docs/adr/ADR-002-build-tool-and-runtime-selection.md)
* **Decision:** Target Java 21 LTS with `spring.threads.virtual.enabled=true` across all services.
* **Why:** Virtual threads unmount from OS carrier threads during blocking I/O (JDBC, HTTP calls), unlocking massive concurrent request handling without the callback complexity and unreadable stack traces of reactive frameworks (WebFlux/Reactor).
* **Trade-off:** Must avoid holding `synchronized` blocks across blocking I/O calls to prevent carrier thread pinning (prefer `ReentrantLock`).

---

## 4. Shared Kernel (`libs/common`)

Every microservice inherits standardized cross-cutting infrastructure from [`libs/common`](file:///Users/adil/Desktop/project-backend-2/libs/common):

1. **Standard Response Envelope (`ApiResponse<T>`)**
   - Uniform payload format for all 2xx success and 4xx/5xx failure responses.
   - Always contains `success` (boolean), `data` (payload), `error` (`ErrorDetails`), `timestamp`, and `correlationId`.

2. **Distributed Tracing Filter (`CorrelationIdFilter`)**
   - `OncePerRequestFilter` with `HIGHEST_PRECEDENCE`.
   - Reads `X-Correlation-Id` header from incoming requests. If absent, generates a secure UUID.
   - Places the ID in SLF4J MDC (`correlationId`) for structured logging.
   - Echoes `X-Correlation-Id` back to the caller in the HTTP response headers.
   - **Invariant:** MDC is cleared in a `finally` block to prevent context leakage across thread pool executions.

3. **Global Exception Handler (`GlobalExceptionHandler`)**
   - `@RestControllerAdvice` catching `BusinessException`, `MethodArgumentNotValidException`, and unhandled `Exception`.
   - Formats validation field errors into structured `ValidationError` arrays.
   - Masks internal stack traces in 500 errors to prevent credential or internal architecture leakage.

---

## 5. Local Infrastructure & Database Provisioning

- **Docker Compose:** [`infra/docker/docker-compose.yml`](file:///Users/adil/Desktop/project-backend-2/infra/docker/docker-compose.yml) runs PostgreSQL 16 on port `5432`.
- **Multi-Database Init Script:** [`infra/docker/init-databases.sh`](file:///Users/adil/Desktop/project-backend-2/infra/docker/init-databases.sh) is mounted to `/docker-entrypoint-initdb.d/`. On the first startup, it automatically provisions:
  1. `user_db`
  2. `catalog_db`
  3. `order_db`
- **Flyway Migrations:** Each microservice manages its own versioned schema independently:
  - `services/user-service/src/main/resources/db/migration/V1__init_user_schema.sql`
  - `services/catalog-service/src/main/resources/db/migration/V1__init_catalog_schema.sql`
  - `services/order-service/src/main/resources/db/migration/V1__init_order_schema.sql`

---

## 6. Testing Strategy & Testcontainers

Tests are first-class citizen verification tools, not an afterthought:
- Every microservice integration test uses **Testcontainers** (`postgres:16-alpine`) via Spring Boot 3.1 `@ServiceConnection`.
- No in-memory databases (H2) are used.
- Tests prove that:
  1. Spring contexts boot with Java 21 Virtual Threads.
  2. Flyway migrations apply cleanly against genuine PostgreSQL engines.
  3. Health endpoints return the standardized `ApiResponse` envelope with distributed correlation IDs.

---

## 7. Verification Runbook

### Build and Test All Modules
```bash
./gradlew test
```

### Start Local Infrastructure (PostgreSQL 16)
```bash
docker compose -f infra/docker/docker-compose.yml up -d
```

### Run an Individual Microservice
```bash
./gradlew :services:user-service:bootRun
# or
./gradlew :services:catalog-service:bootRun
# or
./gradlew :services:order-service:bootRun
```

### Test Health Endpoint with Correlation ID
```bash
curl -i -H "X-Correlation-Id: test-client-trace-123" http://localhost:8081/api/v1/health
```

---

## 8. Learner Task 0.1 Checklist

- [ ] Implement `GET /api/v1/info` in `services/user-service`.
- [ ] Return `app`, `javaVersion`, and `virtualThreads` (check `Thread.currentThread().isVirtual()`).
- [ ] Wrap in `ApiResponse<Map<String, String>>` using `CorrelationIdUtils.getCorrelationId()`.
- [ ] Add `@Test void shouldReturnAppInfoWithVirtualThreadIndicator()` in `UserServiceApplicationTests.java`.
- [ ] Verify test passes with `./gradlew :services:user-service:test`.
