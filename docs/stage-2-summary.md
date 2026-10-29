# Stage 2 Summary: Redis Caching & Distributed State

## Executive Overview
Stage 2 introduces high-performance in-memory caching and distributed coordination using Redis 7 across all microservices, delivering sub-millisecond query performance, sliding-window rate limiting, distributed idempotency locks, and geospatial rider matching.

---

## Implemented Capabilities

### 1. Restaurant Menu Caching & Eviction (`catalog-service`)
- **Technology:** Spring Data Redis, Lettuce connection pool, `RedisCacheManager`.
- **Serialization:** Safe, human-readable JSON serialization via `GenericJackson2JsonRedisSerializer` (rejecting unsafe Java native serialization).
- **TTL & Eviction:** 15-minute default TTL (`restaurants::<id>`), with instantaneous `@CacheEvict` when new menu items are added or updated.
- **Stage 2 Chaos Resilience:** Custom `CacheErrorHandler` catches Redis outages and network partitions, automatically and transparently falling back to direct PostgreSQL reads without surfacing 500 errors to customers. Verified in `RedisOutageChaosIntegrationTests`.

### 2. Sliding-Window Rate Limiting (`user-service`)
- **Technology:** Redis Sorted Sets (`ZSET`).
- **Algorithm:** Dynamic sliding-window log removing expired timestamps via `ZREMRANGEBYSCORE` and counting active hits via `ZCARD`. Eliminates boundary burst vulnerabilities present in fixed-window algorithms.
- **HTTP Integration:** `RateLimitInterceptor` extracts client IP, evaluates `@RateLimit` annotations, and returns HTTP 429 (Too Many Requests) with RFC-compliant `Retry-After: <seconds>` headers.

### 3. Distributed Idempotency Filter (`order-service`)
- **Technology:** Redis `SETNX` (atomic set-if-not-exists) with TTL.
- **Header:** `Idempotency-Key: <UUID>`.
- **Workflow:** Prevents double-billing and duplicate order insertion during mobile network retries. In-flight requests lock with `"__PROCESSING__"`; completed orders cache the full `OrderResponse` for 24 hours, returning `X-Cache-Lookup: HIT` with HTTP 200 on duplicates without writing to `order_db`.

### 4. Rider Geospatial Proximity Dispatching (`dispatch-service`)
- **Technology:** Dedicated `dispatch-service` microservice using Redis Geospatial indexes (`GEOADD`, `GEOSEARCH`).
- **Capabilities:**
  - Real-time rider GPS ping indexing (`POST /api/v1/dispatch/riders/location`).
  - Proximity search (`GET /api/v1/dispatch/riders/nearby`) finding available riders within 5km, sorted by ascending distance.
  - Automated closest-rider dispatch (`POST /api/v1/dispatch/orders/{orderId}/assign-rider`), locking rider state to `ASSIGNED` to prevent double-booking.

---

## Verification & Test Results
- 100% of tests passing with real Testcontainers Redis 7 & PostgreSQL 16 containers across:
  - `RestaurantCacheIntegrationTests` & `RedisOutageChaosIntegrationTests` (`catalog-service`)
  - `RateLimiterIntegrationTests` (`user-service`)
  - `OrderIdempotencyIntegrationTests` (`order-service`)
  - `RiderGeospatialDispatchIntegrationTests` (`dispatch-service`)
