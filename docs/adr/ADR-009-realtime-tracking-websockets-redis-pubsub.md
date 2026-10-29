# ADR-009: Real-Time Location Streaming with STOMP & Redis Backplane

## Status
Accepted

## Context
Food delivery users expect real-time visual tracking of their delivery rider moving towards their address on an interactive map.
Traditional HTTP polling (`GET /orders/{id}/tracking` every 2 seconds) introduces heavy request overhead, SSL handshakes, and database connection pool exhaustion.
Additionally, when scaling WebSocket servers across multiple container instances behind an Application Load Balancer (ALB), a client connected to Server A will not receive location updates pushed by a rider communicating with Server B unless a distributed message backplane is introduced.

## Decisions

### 1. WebSocket with STOMP Protocol
- **Decision:** Use Spring WebSocket (`spring-boot-starter-websocket`) with STOMP (Simple Text Oriented Messaging Protocol) on endpoint `/ws-tracking`.
- **Destination:** Subscriptions route to `/topic/orders/{orderId}/tracking`.
- **Alternatives Considered:**
  - Raw WebSockets: Lacks standardized frame headers, heartbeat semantics, and topic routing, requiring a custom application framing protocol.
  - Server-Sent Events (SSE): Strictly unidirectional (server to client) and does not support bidirectional duplex communication for rider client GPS updates.

### 2. Redis Pub/Sub Distributed Backplane
- **Decision:** Redis Pub/Sub is utilized as the cluster-wide communication backplane.
- **Workflow:**
  - When a rider sends a GPS coordinate (via REST `POST` or STOMP `/app/rider/location`), the receiving `dispatch-service` node publishes the event to Redis channel `tracking:orders:{orderId}`.
  - Every running instance of `dispatch-service` subscribes to `tracking:orders:*` via `RedisMessageListenerContainer`.
  - When a node receives a message from Redis, it checks if it holds active WebSocket sessions for that destination, forwarding the payload via `SimpMessagingTemplate`.

## Consequences
- **Positive:** Reduces server network handshakes by > 90% compared to HTTP polling.
- **Positive:** Flawless horizontal scalability: `dispatch-service` nodes remain stateless; any node can publish, and all subscriber nodes receive updates.
- **Negative:** Redis Pub/Sub does not persist messages (fire-and-forget). For tracking telemetry, transient delivery is ideal since the latest coordinate supersedes historical points.
