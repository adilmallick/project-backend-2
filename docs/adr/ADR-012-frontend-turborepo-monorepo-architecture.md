# ADR-012: Frontend Turborepo Monorepo Architecture with Next.js 15, TypeScript, Tailwind CSS v4 & TanStack Query

## Status
Accepted

## Date
2026-10-30

## Context
With the backend microservices architecture fully operational across 7 stages (Auth, Catalog, Orders, Sagas, Live STOMP Tracking, Observability, and AWS IaC), a unified web client is required to connect to all 4 microservices:
- `user-service` (:8081) - Authentication, JWT session, RBAC
- `catalog-service` (:8082) - Restaurant catalog, menus, Redis-backed query
- `order-service` (:8083) - Order placement, state machine, idempotency keys
- `dispatch-service` (:8084) - Geospatial rider dispatching, STOMP live tracking

We need a scalable, type-safe, and modular frontend repository layout that supports shared domain contracts, efficient incremental builds, and seamless integration with distributed backend services.

## Options Considered

### Option 1: Standalone Single Next.js Application in a Separate Repository
- **Pros:** Isolated CI pipeline, quick initial prototype.
- **Cons:** Disconnected from backend domain contracts and documentation, duplicate type definitions, disjointed developer experience.

### Option 2: Root-level Monorepo Merged Directly into Gradle Root
- **Pros:** Single `package.json` at repository root.
- **Cons:** Pollutes the root directory with Node modules, conflicts with root Gradle build files, and complicates backend Docker builds.

### Option 3: Dedicated `frontend/` Monorepo powered by Turborepo (Chosen)
- **Pros:**
  - **Turborepo Workspace Isolation:** Manages applications (`apps/web`) and shared modular packages (`packages/types`, `packages/config`) with smart dependency graphing and remote caching.
  - **Domain Type Synchronization (`@swiggy/types`):** Shares TypeScript models reflecting Java DTOs (`UserRole`, `Restaurant`, `MenuItem`, `Order`, `OrderStatus`, `ApiResponse<T>`) across current and future clients (e.g. admin portal, rider mobile app).
  - **Next.js 15 App Router & React 19:** High-performance server/client component splitting, optimized asset loading, and SEO metadata.
  - **TanStack Query v5 (React Query):** Declarative server state management, automatic query caching, background polling, retry policies with backoff, and mutations with cache invalidation matching backend Redis TTLs.
  - **Tailwind CSS v4:** Zero-config modern CSS engine with native CSS custom properties, dynamic color palettes, and glassmorphism styling.
  - **Non-destructive Isolation:** Preserves the existing root Gradle build, Docker Compose, and infrastructure scripts untouched.
- **Cons / Trade-offs:**
  - Requires npm workspace configuration and Turborepo pipeline orchestration.

## Decision
We choose **Option 3: Dedicated `frontend/` Monorepo powered by Turborepo**.

- Structure:
  - `frontend/` (Root workspace): `package.json`, `turbo.json`, `tsconfig.base.json`
  - `frontend/packages/types`: Shared TypeScript contracts matching backend API schemas.
  - `frontend/packages/config`: Shared ESLint, Prettier, and TypeScript compiler configurations.
  - `frontend/apps/web`: Next.js 15 App Router application with Tailwind CSS v4, Lucide icons, and TanStack Query v5.

## Consequences

### Positive
- Strict separation of concerns: Backend Gradle workflows remain pristine while frontend gains Turborepo build caching.
- Shared type safety prevents contract drift between microservices and client views.
- Declarative asynchronous data fetching and cache invalidation via TanStack Query mirrors backend caching dynamics.

### Negative / Trade-offs
- Developers must execute `npm install` inside `frontend/` independently from root `./gradlew`.

## Code and Config Affected
- `frontend/package.json`
- `frontend/turbo.json`
- `frontend/tsconfig.base.json`
- `frontend/packages/types/`
- `frontend/packages/config/`
- `frontend/apps/web/`
- `docs/frontend-progress.md`
- `docs/stage-0-frontend-summary.md`
