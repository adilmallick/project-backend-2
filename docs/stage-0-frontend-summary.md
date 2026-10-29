# Stage 0 Frontend Summary: Turborepo Monorepo, Next.js 15 & TanStack Query Foundation

## Executive Overview
Stage 0 establishes the frontend architecture for the Swiggy/Zomato Mini Delivery Platform. Built as a high-performance **Turborepo** monorepo in `frontend/`, it integrates **Next.js 15 (App Router)**, **TypeScript**, **Tailwind CSS v4**, and **TanStack Query v5**. The frontend is wired directly to the backend microservices ecosystem (`user-service`, `catalog-service`, `order-service`, and `dispatch-service`) using shared TypeScript domain contracts and resilient API envelopes.

---

## Completed Architecture & Increments

### 1. Turborepo Workspace Topology (`frontend/`)
- **Structure:**
  - `apps/web`: Next.js 15 App Router web application (port `:3001`).
  - `packages/types`: Shared `@swiggy/types` containing domain contracts matching Java DTOs.
  - `packages/config`: Shared `@swiggy/config` containing service URLs, actuator paths, and TanStack Query keys.
- **Pipeline Orchestration (`turbo.json`):**
  - Declarative topological build, type-check, lint, and dev tasks.
  - Smart inputs/outputs caching (`.next/**`, `dist/**`).

### 2. Domain Contract Synchronization (`@swiggy/types`)
- **Common Envelope:** `ApiResponse<T>`, `ErrorDetails`, `ValidationError`, `ServiceHealth` matching `libs/common`.
- **Identity & RBAC:** `UserRole` (`CUSTOMER`, `RESTAURANT_OWNER`, `RIDER`, `ADMIN`), `AuthResponse`, `UserResponse`.
- **Catalog Domain:** `RestaurantResponse`, `RestaurantSummaryResponse`, `MenuItemResponse`, `CreateRestaurantRequest`.
- **Order Lifecycle:** `OrderStatus` (`ORDER_PLACED`, `RESTAURANT_ACCEPTED`, `RIDER_ASSIGNED`, `PICKED_UP`, `DELIVERED`, `CANCELLED`, `REFUNDED`), `OrderResponse`, `CreateOrderRequest`.
- **Dispatch & Tracking:** `TrackingLocationPayload`, `NearbyRiderResponse`, `UpdateRiderLocationRequest`.

### 3. TanStack Query v5 Architecture (`@tanstack/react-query`)
- **QueryProvider:** Client-side singleton `QueryClient` initialized in `apps/web/src/providers/query-provider.tsx`.
- **Caching Policies:**
  - `staleTime`: 15 minutes (directly matching the backend Redis menu cache TTL defined in ADR-006).
  - `gcTime`: 30 minutes.
  - Devtools: Integrated `@tanstack/react-query-devtools` for inspecting cache state, query keys, and fetch lifecycles.

### 4. Modern Styling & Design System (Tailwind CSS v4)
- **Engine:** `@tailwindcss/postcss` with native `@import "tailwindcss";` in `globals.css`.
- **Theme Aesthetics:** High-fidelity dark mode palette, glassmorphism cards (`backdrop-blur-md`), vibrant saffron/orange (`#fc8019`) and crimson (`#e23744`) brand gradients, glowing border accents, and live status pulse animations.

### 5. Microservices Ingress & Resilient Client
- **Client (`api-client.ts`):** Automatically injects `X-Correlation-Id` for distributed OpenTelemetry tracing, unwraps `ApiResponse<T>`, and handles HTTP 429 rate-limiting.
- **Actuator Health Probes:** Concurrently probes `/actuator/health` across all 4 microservices (:8081, :8082, :8083, :8084) displaying live response latencies.

---

## Architectural Decision Records
- [ADR-012: Frontend Turborepo Monorepo Architecture with Next.js 15, TypeScript, Tailwind CSS v4 & TanStack Query](file:///Users/adil/Desktop/project-backend-2/docs/adr/ADR-012-frontend-turborepo-monorepo-architecture.md)

---

## Next Steps: Stage 1 (Core Domain, Auth & Catalog)
- Implement JWT Auth context with role switching (Customer, Restaurant Owner, Rider, Admin).
- Build paginated Restaurant Catalog view powered by TanStack Query.
- Build Menu Item selection with interactive shopping cart.
- Implement checkout and order placement with Redis-backed distributed idempotency key.
