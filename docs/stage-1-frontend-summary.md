# Stage 1 Frontend Summary: Core Domain, Stateless JWT RBAC & Catalog State

## Executive Overview
Stage 1 implements the complete core business domain for the Swiggy/Zomato Mini Delivery Platform frontend client. It connects user authentication with Role-Based Access Control (RBAC), provides an interactive restaurant catalog backed by TanStack Query, implements an optimistic shopping cart with restaurant boundary enforcement, and enables idempotent order placement matching backend PostgreSQL and Redis invariants.

---

## Completed Architecture & Increments

### 1. Identity, Stateless JWT & RBAC (`context/auth-context.tsx`)
- **Stateless Token Management:** Synchronized JWT storage across React Context and `localStorage`.
- **RBAC Role Switcher:** Instantaneous switching between all 4 domain roles (`CUSTOMER`, `RESTAURANT_OWNER`, `RIDER`, `ADMIN`) with pre-seeded test accounts:
  - `CUSTOMER`: Alice Johnson (`d3b07384-d113-40e1-bbd5-f86749964d7d`)
  - `RESTAURANT_OWNER`: Chef Rajesh Sharma (`b1b07384-d113-40e1-bbd5-f86749964d7b`)
  - `RIDER`: Vikram Singh (`c2b07384-d113-40e1-bbd5-f86749964d7c`)
  - `ADMIN`: Platform Super Admin (`a0b07384-d113-40e1-bbd5-f86749964d7a`)
- **REST Endpoints:** Connected to `POST /api/v1/auth/login` and `POST /api/v1/auth/register` on `user-service` (:8081).

### 2. Restaurant & Menu Catalog (`components/catalog/restaurant-grid.tsx`)
- **TanStack Query Caching:** `useQuery` querying `GET /api/v1/restaurants` from `catalog-service` (:8082).
- **Cache TTL Policy:** Configured with a 15-minute `staleTime` mirroring the backend Redis menu cache policy (ADR-006).
- **Filtering & Search:** Real-time client-side search across restaurant names, descriptions, and addresses with cuisine filters (North Indian, Biryani, Italian, South Indian).
- **Menu Drawer:** Detailed view showing all available dishes, prices in cents formatted to INR/USD, vegetarian indicators, and quantity counters.

### 3. Shopping Cart & Idempotency Checkout (`context/cart-context.tsx` & `components/cart/cart-drawer.tsx`)
- **Restaurant Boundary Invariant:** Enforces that cart items originate from a single restaurant at a time. If a user selects an item from a different restaurant, a clean confirmation dialog prevents accidental cross-restaurant contamination.
- **Dynamic Pricing:** Subtotal, quantity counters, and free delivery tier calculations.
- **Distributed Idempotency (ADR-007):** Checkout automatically generates a unique UUID v4 `Idempotency-Key` sent via HTTP headers (`Idempotency-Key` and `X-Idempotency-Key`) to `order-service` (:8083). This prevents duplicate order creation on network retries or rapid double-clicks.
- **State Machine Timeline (`components/orders/orders-tracker.tsx`):** Displays real-time order lifecycle status (`ORDER_PLACED` -> `RESTAURANT_ACCEPTED` -> `RIDER_ASSIGNED` -> `PICKED_UP` -> `DELIVERED`).

---

## Architectural Decision Records
- [ADR-013: Frontend Core Domain, Stateless JWT RBAC & Catalog State](file:///Users/adil/Desktop/project-backend-2/docs/adr/ADR-013-frontend-core-domain-auth-and-catalog.md)

---

## Checkpoint Verification
- **TypeScript:** Strict type checks passing across all packages (`@swiggy/types`, `@swiggy/config`, `web`).
- **Build:** Next.js production build compiling cleanly with zero errors.
- **Browser Simulation:** Interactive cart addition, menu browsing, and role switching verified.
