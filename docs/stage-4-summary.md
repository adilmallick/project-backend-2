# Stage 4 Summary: Live Tracking & WebSockets

## Executive Overview
Stage 4 implements bidirectional, real-time rider tracking and telemetry streaming using Spring WebSocket, STOMP protocol, and a horizontally scalable Redis Pub/Sub cluster backplane.

---

## Implemented Architecture & Capabilities

### 1. Full-Duplex STOMP over WebSocket (`dispatch-service`)
- **Protocol & Endpoint:** STOMP protocol exposed at `/ws-tracking` with SockJS fallback support.
- **Client Subscription:** Clients subscribe to dynamic order channels: `/topic/orders/{orderId}/tracking`.
- **Payload Schema:** `TrackingLocationPayload` streaming `orderId`, `riderId`, `latitude`, `longitude`, `status`, and `timestamp`.

### 2. Horizontally Scalable Redis Pub/Sub Backplane
- **Problem Solved:** When users and riders connect to different load-balanced container instances, local in-memory message brokers cannot bridge across nodes.
- **Backplane Routing:**
  - Ingress telemetry updates broadcast to Redis channel `tracking:orders:{orderId}`.
  - `RedisMessageListenerContainer` across all running `dispatch-service` instances catches cluster broadcasts.
  - `RedisTrackingSubscriber` forwards the message to connected WebSocket clients via `SimpMessagingTemplate`.

### 3. Dual Ingress (REST & WebSocket)
- REST: `POST /api/v1/dispatch/orders/{orderId}/tracking` for battery-efficient background GPS pings.
- STOMP: `@MessageMapping("/rider/location")` for high-frequency live rider map tracking.

---

## Verification & Test Results
- `WebSocketTrackingIntegrationTests` passing: Established live WebSocket connection using `StandardWebSocketClient`, subscribed to STOMP destination, published update via Redis Pub/Sub backplane, and asserted frame reception within milliseconds.
