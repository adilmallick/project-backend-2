# ADR-008: Transactional Outbox Pattern & Event-Driven Sagas

## Status
Accepted

## Context
In a distributed microservices platform, business operations frequently span database mutations and message publishing (e.g., placing an order and notifying payment/restaurant/rider services).
A direct dual-write approach (`database.save(order)` followed by `kafka.send(event)`) introduces fatal distributed failure modes:
1. **DB commits, Kafka send fails:** Order exists in the database, but downstream systems are never notified (silent lost message).
2. **Kafka send succeeds, DB transaction rolls back:** Downstream systems charge the customer's card for an order that was aborted in the database.
Distributed two-phase commits (2PC / XA transactions) are not supported by modern message brokers and introduce unacceptable latency and blocking locks.

## Decisions

### 1. Transactional Outbox Pattern
- **Decision:** Every domain mutation writes both the entity (e.g., `Order`) and an `OutboxEvent` record to PostgreSQL within the exact same ACID transaction (`@Transactional`).
- **Schema:** An `outbox_events` table stores `id`, `aggregate_type`, `aggregate_id`, `event_type`, `payload` (JSON), `status` (`PENDING`, `PUBLISHED`, `FAILED`), and timestamps.
- **Guarantee:** At-least-once message delivery. If the database transaction commits, the event is guaranteed to be persisted.

### 2. Asynchronous Outbox Publisher
- **Decision:** An asynchronous worker (`OutboxPublisherService`) polls pending events and streams them to Apache Kafka using `aggregate_id` as the message key.
- **Key-Based Partitioning:** Kafka routes all events for a given order to the exact same partition, guaranteeing strict chronological ordering (FIFO) per order.
- **Acknowledgment:** Only after Kafka acknowledges persistent commit does the outbox status transition to `PUBLISHED`.
- **Fault Tolerance:** If Kafka experiences network partitions or downtime, outbox events remain safely staged in `outbox_events` with incrementing `retry_count`. Once Kafka recovers, the queue drains with zero message loss (verified in `OrderOutboxChaosIntegrationTests`).

### 3. Choreographed Saga with Compensating Transactions
- **Decision:** We implement a Choreographed Saga for order fulfillment without a centralized blocking orchestrator.
- **Forward Path:** `OrderPlacedEvent` -> `PaymentServiceSimulator` charges card -> publishes `PaymentEvent(success = true)` -> `OrderSagaCoordinator` transitions order to `RESTAURANT_ACCEPTED`.
- **Compensating Path:** When payment fails (e.g., insufficient funds), `PaymentEvent(success = false)` triggers a compensating transaction in `OrderSagaCoordinator`, transitioning order to `CANCELLED` and alerting the user.

## Consequences
- **Positive:** Elimination of the Dual-Write anomaly with zero cross-system distributed lock overhead.
- **Positive:** High availability: Orders can continue to be placed even during complete Kafka broker downtime.
- **Negative:** Downstream consumers must be idempotent (addressed via ADR-007 idempotency filters and consumer deduplication).
