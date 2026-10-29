# Stage 1 Summary: Core Domain, Security & Concurrency

## Executive Overview
Stage 1 establishes the production domain cores and security boundaries for three independent microservices (`user-service`, `catalog-service`, and `order-service`), adhering strictly to Database-per-Service isolation, modern Java 21 Virtual Threads, Spring Boot 3.4, and clean RESTful API design.

---

## Completed Increments

### Increment 1.1: `user-service` (Identity & JWT Security)
- **Schema:** Flyway migration `V1__init_user_schema.sql` defining `users` table with UUID primary keys and `roles` array.
- **Security:**
  - BCrypt password hashing with cost factor 12.
  - Stateless HMAC-SHA256 JWT tokens using JJWT 0.12.6.
  - Custom `JwtAuthenticationFilter` reading `Authorization: Bearer <token>` and populating Spring Security `SecurityContextHolder`.
  - Granular RBAC (`CUSTOMER`, `RESTAURANT_OWNER`, `RIDER`, `ADMIN`).
- **Tests:** 8 Testcontainers integration tests verifying registration, login, wrong passwords, and authenticated `/api/v1/users/me` extraction.
- **Architectural Record:** [ADR-003: Stateless JWT Authentication with RBAC](file:///Users/adil/Desktop/project-backend-2/docs/adr/ADR-003-stateless-jwt-authentication.md).

### Increment 1.2: `catalog-service` (Domain & N+1 Prevention)
- **Schema:** Flyway migration `V1__init_catalog_schema.sql` modeling `restaurants` and `menu_items` with foreign keys and active flags.
- **JPA Optimization:**
  - Demonstrated the classic N+1 query antipattern in `GET /api/v1/restaurants/unoptimized` (executing 1 query for restaurants + N queries for menu items).
  - Implemented `@EntityGraph(attributePaths = {"menuItems"})` in `GET /api/v1/restaurants`, fetching restaurants and menu items in exactly 1 SQL query with `LEFT OUTER JOIN`.
- **Search:** Case-insensitive search with pagination via Spring Data `Pageable`.
- **Tests:** 5 Testcontainers integration tests verifying query counts and search functionality.
- **Architectural Record:** [ADR-004: JPA Query Optimization and N+1 Prevention](file:///Users/adil/Desktop/project-backend-2/docs/adr/ADR-004-jpa-query-optimization-and-n-plus-1-prevention.md).

### Increment 1.3: `order-service` (State Machine & Optimistic Locking)
- **Schema:** Flyway migration `V1__init_order_schema.sql` with `orders` (including `@Version bigint NOT NULL DEFAULT 0`) and `order_items`.
- **Domain Modeling:**
  - Explicit State Machine: `ORDER_PLACED` -> `RESTAURANT_ACCEPTED` -> `RIDER_ASSIGNED` -> `PICKED_UP` -> `DELIVERED` (with terminal `CANCELLED`). Illegal transitions throw `BusinessException("INVALID_STATUS_TRANSITION")`.
  - Point-in-time Price Snapshotting: Order items snapshot menu item prices and names at checkout time; future catalog price changes do not mutate historical order records.
  - Optimistic Locking: `@Version` column prevents the Lost Update anomaly without holding heavy database row locks.
- **Tests:** 5 Testcontainers integration tests verifying lifecycle transitions, invalid transition rejection, and a concurrent chaos test proving `OptimisticLockingFailureException` under simultaneous writes.
- **Architectural Record:** [ADR-005: Order State Machine & Optimistic Locking Concurrency](file:///Users/adil/Desktop/project-backend-2/docs/adr/ADR-005-order-state-machine-concurrency.md).

### OpenAPI 3 & Swagger UI Integration
- Added `springdoc-openapi-starter-webmvc-ui:2.8.5` in `libs/common`.
- Configured unified `OpenApiConfig` with JWT Bearer security scheme so every microservice exposes interactive Swagger UI at `/swagger-ui.html` and OpenAPI spec at `/v3/api-docs`.

---

## Stage 1 Checkpoint Verification
- **Build & Test Suite:** 19 Gradle tasks executed, 100% tests passing across all microservices using real PostgreSQL 16 Testcontainers.
- **Commit:** Clean git history with atomic commits per increment.
