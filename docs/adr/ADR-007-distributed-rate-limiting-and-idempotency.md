# ADR-007: Distributed Rate Limiting & Idempotency using Redis

## Status
Accepted

## Context
High-traffic food delivery backends face two distributed state challenges:
1. **Brute-Force & Abuse Prevention:** Authentication endpoints (`/api/v1/auth/login`) require rate limiting across horizontally scaled instances without sticky sessions.
2. **Double-Order / Network Retry Prevention:** Mobile clients with unstable mobile connections frequently retry `POST /api/v1/orders`, risking duplicate orders and double payments.

## Decisions

### 1. Sliding-Window Rate Limiting via Redis Sorted Sets (ZSET)
- **Decision:** Use Redis `ZSET` to implement a sliding-window rate limiter per client IP / route.
- **Algorithm:**
  - Key: `rate_limit:{clientIp}:{uri}`
  - `ZREMRANGEBYSCORE key 0 (now - windowMs)` evicts timestamps older than sliding window.
  - `ZCARD key` inspects requests within the current window.
  - If below threshold: `ZADD key now uniqueMember` and `EXPIRE key (windowSeconds + 5)`.
  - If exceeded: Reject with HTTP 429 Too Many Requests and RFC-compliant `Retry-After` header.
- **Alternatives Considered:**
  - Fixed-Window Counter (`INCR`): Suffers from boundary burst anomaly (allowing 2x traffic during window boundaries).
  - Token Bucket: Requires floating-point arithmetic or Lua scripting; ZSET provides exact microsecond precision with zero Lua script dependency.

### 2. Distributed Idempotency via Redis SETNX
- **Decision:** Client provides an `Idempotency-Key` header on order submission.
- **Workflow:**
  - Atomic `SET idempotency:order:{key} "__PROCESSING__" NX EX 120` acquires the distributed execution lock.
  - If key exists:
    - If value is `"__PROCESSING__"`, another request is in flight -> HTTP 409 Conflict (`CONCURRENT_REQUEST`).
    - If value is serialized `OrderResponse`, return cached response immediately (`X-Cache-Lookup: HIT`) without hitting PostgreSQL.
  - If lock acquired: Process order in PostgreSQL, then save serialized JSON response with 24-hour retention. On error, release the lock so the client can safely retry.

### 3. Rider Geospatial Proximity Matching (Redis GEO)
- **Decision:** Real-time rider coordinates are indexed in Redis using `GEOADD riders:locations <lon> <lat> <riderId>`. Nearest available riders are discovered via `GEOSEARCH` within a specified radius (e.g. 5km), sorted by proximity in $O(N + \log M)$ time.

## Consequences
- **Positive:** Elimination of boundary spikes in rate limiting.
- **Positive:** Guaranteed zero double-orders or double-charges during network packet retransmissions.
- **Positive:** Dispatch algorithm selects closest rider in < 1ms in memory.
- **Negative:** Requires active Redis connection; rate limiter uses fail-open resilience in the event of complete cache cluster failure.
