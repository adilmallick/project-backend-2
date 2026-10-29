# ADR-001: Multi-Project Microservices Architecture from Day 1

## Status
Accepted (Supersedes preliminary single-monolith discussion)

## Date
2026-10-29

## Context
We are engineering a distributed food-delivery platform (Swiggy/Zomato style) with distinct sub-domains: User/Auth, Restaurant Catalog, Order Lifecycle & Saga, Payment, Dispatch, and Live Tracking.

We evaluated two architectural approaches:
1. **Modular Monolith (Spring Modulith):** A single deployable JAR with internal package encapsulation, splitting into microservices later.
2. **Multi-Project Microservices Monorepo:** Independent Spring Boot microservices (`user-service`, `catalog-service`, `order-service`) running in a single repository with a shared common library (`libs/common`). Each service has its own runtime, port, database schema, and test suite from Day 1.

## Options Considered

### Option 1: Modular Monolith
- **Pros:** Zero network latency, single JVM process, single database pool.
- **Cons:** Defers learning distributed network boundaries, multi-service Docker orchestration, and database-per-service isolation patterns.

### Option 2: Multi-Project Microservices Monorepo (Chosen)
- **Pros:**
  - **True Operational Isolation:** Each service (`user-service`, `catalog-service`, `order-service`) runs as an independent Spring Boot application.
  - **Database-per-Service:** Enforces strict boundary isolation—`order-service` physically cannot perform cross-database SQL joins against `user_db` or `catalog_db`.
  - **Unified Build & Versioning:** Gradle multi-project build allows compiling, testing, and verifying contracts across all services in a single workflow.
  - **Realistic Distributed Failure Modes:** Exposes network timeouts, serialization nuances, and service discovery from Stage 1.
- **Cons / Trade-offs:**
  - Higher local resource consumption (running multiple JVMs). Mitigated by Java 21 and tuned container footprints.
  - Requires Docker Compose service orchestration and inter-service HTTP/event communication.

## Decision
We choose **Option 2: Multi-Project Microservices Monorepo**.
- Structure:
  - `libs/common`: Shared cross-cutting models, DTOs, correlation ID filters, error handling.
  - `services/user-service`: Port 8081, PostgreSQL `user_db`.
  - `services/catalog-service`: Port 8082, PostgreSQL `catalog_db`.
  - `services/order-service`: Port 8083, PostgreSQL `order_db`.
- Infrastructure:
  - PostgreSQL 16 container initializing isolated databases for each service (`user_db`, `catalog_db`, `order_db`).

## Consequences

### Positive
- Strict physical separation of domain boundaries from commit #1.
- Immediate mastery of microservice build management, multi-service Docker Compose, and independent Testcontainers suites.
- Direct alignment with target production architecture (ECS Fargate / EKS in Stage 6).

### Negative / Trade-offs
- Multiple running Spring Boot processes locally.
- Requires database-per-service init scripts.

## Code and Config Affected
- `settings.gradle.kts` (includes `:libs:common`, `:services:user-service`, `:services:catalog-service`, `:services:order-service`)
- `build.gradle.kts` (subproject conventions)
- `infra/docker/init-databases.sh` (provisions `user_db`, `catalog_db`, `order_db`)
- `infra/docker/docker-compose.yml`
