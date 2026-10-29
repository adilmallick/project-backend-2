# ADR-006: Redis Cache Eviction & Serialization Strategies

## Status
Accepted

## Context
Catalog queries for restaurant menus represent over 70% of the platform read traffic. While the Stage 1 `@EntityGraph` optimization reduced SQL query round trips from $1 + N$ to 1 single query, recurring relational database queries for popular restaurants create unnecessary CPU and connection contention on PostgreSQL.
We need an in-memory caching layer that provides sub-millisecond retrieval, predictable TTLs, safe serialization, and instant invalidation upon menu updates. Furthermore, we must ensure high system resiliency during Redis cluster failures.

## Decisions

### 1. JSON Serialization (GenericJackson2JsonRedisSerializer)
- **Decision:** All cached entries in Redis are serialized as JSON with Jackson type metadata.
- **Alternatives Considered:**
  - `JdkSerializationRedisSerializer` (Java Native Serialization): Rejected due to critical security risks (remote code execution vulnerabilities on untrusted deserialization) and unreadable binary payloads in `redis-cli`.
  - Binary Protocol Buffers / Avro: High performance, but adds schema compilation overhead for internal read cache models.
- **Trade-off:** JSON payloads are marginally larger than binary serialization, but human-readable inspection via `redis-cli`, cross-language compatibility, and zero JVM vulnerability make Jackson the industry standard.

### 2. Time-To-Live (TTL) & Cache Invalidation
- **Decision:** A baseline TTL of 15 minutes (`Duration.ofMinutes(15)`) is enforced on restaurant cache keys (`restaurants::<id>`).
- **Invalidation Strategy:** Whenever a menu item is added or modified, `@CacheEvict(value = "restaurants", key = "#restaurantId")` is executed, immediately expunging stale entries.

### 3. Graceful Fallback on Redis Outage (Resilient CacheErrorHandler)
- **Decision:** Implemented custom `CacheErrorHandler` in `RedisCacheConfig`. When Redis node crashes, times out, or experiences a network partition, the handler logs a `WARN` message and bypasses cache read/write, allowing Spring Boot to fetch data directly from PostgreSQL.
- **Verification:** Verified in `RedisOutageChaosIntegrationTests`.

## Consequences
- **Positive:** P99 restaurant menu read latencies drop from ~12ms (Postgres) to < 1ms (Redis).
- **Positive:** Zero 500 errors served to customers during Redis downtime.
- **Negative:** Memory consumption on Redis instance scales linearly with active restaurant count.
