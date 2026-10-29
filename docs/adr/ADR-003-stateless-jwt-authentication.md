# ADR-003: Stateless JWT Authentication with Role-Based Access Control

## Status
Accepted

## Date
2026-10-30

## Context
In our microservices architecture, clients (Customers, Restaurant Owners, Delivery Riders) interact with multiple independent services (`user-service`, `catalog-service`, `order-service`). We need a secure, scalable authentication mechanism that validates identity and authorizes role-specific actions without introducing a database bottleneck or shared session state.

## Options Considered

### Option 1: Stateful Server-Side Sessions (Redis / Spring Session)
- **Mechanism:** User logs in, a random session token is issued, and session data (`userId`, `role`) is stored in a centralized Redis cluster or database table.
- **Pros:** Instant server-side revocation (deleting the session key immediately logs out the user).
- **Cons:** Every single HTTP request across every microservice requires a network call to the central session store. If the session store experiences latency spikes or goes down, all authenticated traffic halts.

### Option 2: Stateless JSON Web Tokens (JWT) (Chosen)
- **Mechanism:** User logs in with email and password. `user-service` validates the BCrypt password hash and signs a JWT containing claims (`sub`, `email`, `role`, `exp`). Subsequent requests send `Authorization: Bearer <token>`.
- **Pros:**
  - **Zero Database / Network Calls on Verification:** Any microservice with the signing key can mathematically verify token authenticity in memory in microseconds.
  - **Horizontal Scalability:** Perfectly aligns with stateless microservices and Java 21 virtual threads.
- **Cons / Trade-offs:**
  - Cannot instantly revoke an active access token before it reaches its expiration timestamp (`exp`).
  - **Mitigation:** Enforce short access token expiration (15 minutes). Longer sessions are handled via refresh tokens or Redis blocklists in later stages.

## Decision
We choose **Option 2: Stateless JWT Authentication** using HMAC-SHA256 (`HS256`) with **BCrypt password hashing** (cost factor 12) in `user-service`.

## Consequences
### Positive
- High-throughput authentication verification with zero database I/O overhead per request.
- Independent microservices can authenticate requests locally without cross-service network hops.
- Role claims (`CUSTOMER`, `RESTAURANT_OWNER`, `RIDER`, `ADMIN`) are carried in the payload for immediate RBAC evaluation.

### Negative / Trade-offs
- Tokens cannot be revoked before expiration without maintaining a distributed revocation blocklist (deferred to Stage 2 with Redis).
- Token payload is readable (Base64URL encoded); sensitive secrets (e.g. passwords, SSNs) must never be placed in token claims.

## Code and Config Affected
- `services/user-service/build.gradle.kts` (dependencies: `spring-boot-starter-security`, `jjwt-api`, `jjwt-impl`, `jjwt-jackson`)
- `services/user-service/src/main/java/com/swiggy/user/security/` (`JwtService`, `JwtAuthenticationFilter`, `SecurityConfig`)
- `services/user-service/src/main/java/com/swiggy/user/service/AuthService.java`
- `services/user-service/src/main/resources/application.yml` (`security.jwt.secret`, `security.jwt.expiration-minutes`)
