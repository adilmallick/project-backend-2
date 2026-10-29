# Stage 3 Summary: Kafka & Event-Driven Sagas

## Executive Overview
Stage 3 addresses distributed consistency, dual-write anomaly prevention, and asynchronous event choreography using Apache Kafka (KRaft mode) and the Transactional Outbox Pattern.

---

## Implemented Architecture & Capabilities

### 1. Transactional Outbox Pattern (`order-service`)
- **Schema:** Flyway migration `V2__init_outbox_schema.sql` defining `outbox_events` table with index on `(status, created_at)`.
- **Atomic Staging:** In `OrderService.createOrder`, the `Order` entity and the `OutboxEvent` are committed in the **exact same ACID PostgreSQL transaction**, eliminating dual-write race conditions.
- **Payload:** Structured JSON payload containing `OrderPlacedEvent` metadata (`orderId`, `customerId`, `restaurantId`, `totalAmountCents`, `createdAt`).

### 2. Reliable Outbox Publisher (`order-service`)
- **Relay Worker:** `OutboxPublisherService` queries pending outbox records and sends messages to Kafka topic `order.events`.
- **Partitioning Invariant:** Messages are keyed with `aggregateId` (`orderId`), ensuring strict per-order chronological ordering across Kafka partitions.
- **Safe State Transition:** Only updates event status to `PUBLISHED` after Kafka explicitly acknowledges receipt.
- **Broker Chaos Resilience:** If Kafka is temporarily unreachable, events remain `PENDING` with retry tracking; upon broker recovery, the publisher resumes draining without data loss.

### 3. Choreographed Order Saga with Compensating Transactions
- **Simulated Payment Gateway:** `PaymentServiceSimulator` consumes `order.events` and publishes `PaymentEvent` to `payment.events`. Simulates payment success (< ₹1,000) or simulated failure (>= ₹1,000).
- **Saga Coordinator:** `OrderSagaCoordinator` consumes `payment.events`:
  - **Happy Path:** Advances order to `RESTAURANT_ACCEPTED`.
  - **Compensating Transaction:** When payment declines, rolls back the saga by transitioning the order to `CANCELLED`.

---

## Verification & Test Results
- 100% of tests passing with real Testcontainers Kafka (`confluentinc/cp-kafka:7.6.1`) and PostgreSQL 16:
  - `OrderTransactionalOutboxSagaIntegrationTests`: Verified atomic outbox staging, Kafka event relay, payment success path, and payment failure compensating transaction.
  - `OrderOutboxChaosIntegrationTests`: Verified order placement resilience during complete Kafka broker downtime.
