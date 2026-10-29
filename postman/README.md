# Postman Collection & Environment Guide 📮🚀

This directory contains the production-ready Postman collection and local environment for testing the **Swiggy/Zomato Mini Delivery Platform**.

---

## 📁 Files Included

| File | Purpose | Description |
| :--- | :--- | :--- |
| [`swiggy-platform.postman_collection.json`](swiggy-platform.postman_collection.json) | **Postman Collection v2.1.0** | 33 requests organized into 6 logical folders with automated JavaScript tests and parameter chaining. |
| [`swiggy-local.postman_environment.json`](swiggy-local.postman_environment.json) | **Postman Environment** | Pre-configured environment variables for local microservices running on ports `:8081`, `:8082`, `:8083`, and `:8084`. |

---

## 🗂️ Collection Structure

```text
Swiggy Mini Delivery Platform API/
├── 0. System Health & Probes
│   ├── User Service Health (:8081)
│   ├── Catalog Service Health (:8082)
│   ├── Order Service Health (:8083)
│   └── Dispatch Service Actuator Health (:8084)
├── 1. User Service (:8081) - Auth & Profiles
│   ├── 1. Register Customer (Auto-extracts JWT, user_id)
│   ├── 2. Register Restaurant Owner (Auto-extracts owner_id)
│   ├── 3. Register Delivery Rider (Auto-extracts rider_id)
│   ├── 4. Login (Customer)
│   ├── 5. Get Current User Profile (/me) [Bearer JWT]
│   ├── 6. Get User By ID [Bearer JWT]
│   └── 7. Rate Limit Probe (Redis Sliding Window)
├── 2. Catalog Service (:8082) - Restaurants & Menus
│   ├── 1. Create Restaurant (Auto-extracts restaurant_id)
│   ├── 2. Add Menu Item 1 (Special Chicken Biryani) (Auto-extracts menu_item_id)
│   ├── 3. Add Menu Item 2 (Butter Garlic Naan)
│   ├── 4. Get All Restaurants (Optimized - @EntityGraph & Redis Cache)
│   ├── 5. Get All Restaurants (Unoptimized Demonstration - N+1 Queries)
│   ├── 6. Get Restaurant By ID (Cached with @CacheEvict)
│   └── 7. Search Restaurants (Paginated)
├── 3. Order Service (:8083) - State Machine & Sagas
│   ├── 1. Place Order (with Idempotency Key & Outbox) (Auto-extracts order_id)
│   ├── 2. Verify Distributed Idempotency (Replay Same Key - Cache HIT)
│   ├── 3. Get Order By ID
│   ├── 4. Transition Status -> RESTAURANT_ACCEPTED
│   ├── 5. Transition Status -> RIDER_ASSIGNED
│   ├── 6. Transition Status -> PICKED_UP
│   ├── 7. Transition Status -> DELIVERED (Terminal State)
│   ├── 8. Invalid Transition Rejection Guard (Expect 400 Bad Request)
│   └── 9. Get Orders By Customer
├── 4. Dispatch Service (:8084) - Geospatial & Live Tracking
│   ├── 1. Update Rider GPS Location (Redis GEOADD)
│   ├── 2. Find Nearby Riders (Redis GEOSEARCH within 5 km)
│   ├── 3. Assign Nearest Rider to Order
│   ├── 4. Post Live GPS Telemetry Breadcrumb (REST Ingress)
│   └── 5. WebSocket STOMP Endpoint Info (SockJS Handshake)
└── 5. Observability, Actuator & Resilience
    ├── 1. Order Service - Actuator Health (Detailed)
    ├── 2. Order Service - Micrometer Metrics List
    ├── 3. Order Service - Custom Metric: Orders Created Total
    ├── 4. Order Service - Resilience4j Circuit Breakers
    ├── 5. Order Service - Prometheus Scrape Endpoint
    ├── 6. OpenAPI 3 Spec - User Service
    ├── 7. OpenAPI 3 Spec - Catalog Service
    ├── 8. OpenAPI 3 Spec - Order Service
    └── 9. OpenAPI 3 Spec - Dispatch Service
```

---

## ⚡ Quick Start: How to Import into Postman

1. Open the **Postman** desktop application or web client.
2. Click **Import** (top left).
3. Drag & drop or select:
   - `postman/swiggy-platform.postman_collection.json`
   - `postman/swiggy-local.postman_environment.json`
4. In the top-right environment dropdown, select **`Swiggy Mini Platform - Local Dev`**.
5. Start running the requests!

---

## 🔄 Dynamic Variable Chaining

All requests include Postman **Tests** scripts that extract IDs and tokens from responses and automatically populate collection variables:

| Variable | Set By Request | Consumed By |
| :--- | :--- | :--- |
| `jwt_token` | `1. Register Customer` or `4. Login` | All protected endpoints (`/users/me`, `/users/{id}`) |
| `user_id` | `1. Register Customer` | `Create Restaurant` (as `ownerId`), `Place Order` (as `customerId`), `Get Orders By Customer` |
| `owner_id` | `2. Register Restaurant Owner` | `Create Restaurant` |
| `rider_id` | `3. Register Delivery Rider` | `Update Rider GPS Location`, `Tracking Breadcrumb` |
| `restaurant_id` | `1. Create Restaurant` | `Add Menu Item`, `Get Restaurant By ID`, `Place Order` |
| `menu_item_id` | `2. Add Menu Item 1` | `Place Order` (line items) |
| `order_id` | `1. Place Order` | `Get Order By ID`, all State Machine transitions, `Assign Rider`, `Tracking Breadcrumb` |
| `current_idempotency_key` | Auto-generated before `Place Order` | `Verify Distributed Idempotency` (validates Redis `X-Cache-Lookup: HIT`) |

---

## 🧪 Running via CLI (Newman)

You can run the entire test suite headlessly using **Newman**:

```bash
# Install newman if needed
npm install -g newman

# Run the collection against the local environment
newman run postman/swiggy-platform.postman_collection.json \
  -e postman/swiggy-local.postman_environment.json \
  --reporters cli
```
