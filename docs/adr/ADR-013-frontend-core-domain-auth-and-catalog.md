# ADR-013: Frontend Core Domain, Stateless JWT RBAC & Catalog State

## Status
Accepted

## Date
2026-10-30

## Context
In Stage 1 of the platform, the backend established the core business domain:
- `user-service` (:8081): Stateless HMAC-SHA256 JWT authentication, BCrypt hashing, and 4 domain roles (`CUSTOMER`, `RESTAURANT_OWNER`, `RIDER`, `ADMIN`).
- `catalog-service` (:8082): Restaurant & MenuItem catalog, `@EntityGraph` N+1 query prevention, paginated retrieval.
- `order-service` (:8083): Order placement with point-in-time pricing snapshots, optimistic concurrency control (`@Version`), and idempotency keys.

The frontend client must implement seamless role-based authentication, paginated catalog discovery, resilient shopping cart state, and idempotent order placement that respects the backend invariants.

## Options Considered

### Option 1: Full-Page SSR Reloads on Every Interaction
- **Pros:** Minimal client JavaScript state.
- **Cons:** High latency, poor mobile UX, loses cart state on navigation, violates modern web app expectations.

### Option 2: Monolithic Redux Store for All Server & Client State
- **Pros:** Centralized state tree.
- **Cons:** Boilerplate heavy, duplicates server state caching already solved by TanStack Query, poor separation between server cache and client-only UI state.

### Option 3: TanStack Query Server State + Lightweight React Context for Auth & Cart (Chosen)
- **Pros:**
  - **Clean Separation of Concerns:** Server state (restaurants, menus, orders, user profile) is managed declaratively by TanStack Query with automatic background refetching and cache invalidation.
  - **Client Contexts for Fast Interactive State:** `AuthContext` manages the active JWT token, user claims, and role switcher; `CartContext` manages optimistic shopping cart additions and restaurant boundary validation.
  - **Idempotency by Design:** Cart checkout generates a unique UUID v4 `X-Idempotency-Key` passed to `order-service` (:8083), ensuring double-click or retry requests do not result in duplicate billing or orders.
  - **Zero External UI Bloat:** Fast, lightweight, type-safe implementation using shared `@swiggy/types`.
- **Cons / Trade-offs:**
  - Requires token persistence management across browser reloads (solved via synchronized `localStorage` + React state).

## Decision
We choose **Option 3: TanStack Query Server State + Lightweight React Context for Auth & Cart**.

- Structure:
  - `apps/web/src/context/auth-context.tsx`: Manages JWT access token, user session, login, registration, and demo role switching.
  - `apps/web/src/context/cart-context.tsx`: Manages cart items, price calculations, single-restaurant boundary checks, and checkout modal state.
  - `apps/web/src/components/catalog/`: Restaurant cards, search bar, cuisine filters, and menu item modal.
  - `apps/web/src/components/cart/`: Cart slide-over drawer, quantity controls, delivery address input, and idempotent checkout button.
  - `apps/web/src/components/auth/`: Login/Register modal with demo role presets.

## Consequences

### Positive
- Direct adherence to backend Stage 1 contracts (`UserRole`, `RestaurantResponse`, `OrderResponse`).
- Instantaneous UI response for cart operations while server state stays in sync with backend Redis/PostgreSQL.
- Network-retry resilience via automated `X-Idempotency-Key` headers matching backend Redis SETNX (ADR-007).

### Negative / Trade-offs
- LocalStorage token storage requires standard XSS protection (mitigated by sanitized inputs and avoiding raw HTML rendering).

## Code and Config Affected
- `docs/frontend-progress.md`
- `docs/stage-1-frontend-summary.md`
- `apps/web/src/context/auth-context.tsx`
- `apps/web/src/context/cart-context.tsx`
- `apps/web/src/components/auth/auth-modal.tsx`
- `apps/web/src/components/catalog/restaurant-grid.tsx`
- `apps/web/src/components/cart/cart-drawer.tsx`
- `apps/web/src/app/page.tsx`
