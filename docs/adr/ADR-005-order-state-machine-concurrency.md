# ADR-005: Order State Machine and Optimistic Locking Concurrency

## Status
Accepted

## Date
2026-10-30

## Context
In a multi-actor food delivery platform (Customer, Restaurant, Delivery Rider), orders progress through an asynchronous lifecycle:
`ORDER_PLACED -> RESTAURANT_ACCEPTED -> RIDER_ASSIGNED -> PICKED_UP -> DELIVERED`
along with failure and terminal compensation paths (`CANCELLED`, `REFUNDED`).

We face two architectural challenges:
1. **Invalid State Transitions:** A customer must not be able to cancel an order after it has been picked up; an order must never transition backward (e.g., from `DELIVERED` to `ORDER_PLACED`).
2. **Concurrent Modifications (The Lost Update Anomaly):** A customer may click "Cancel Order" at the exact millisecond the restaurant owner taps "Accept Order". Without concurrency control, whichever transaction commits last blindly overwrites the previous state.

## Options Considered

### Concurrency Control
1. **Pessimistic Locking (`SELECT ... FOR UPDATE`):**
   - **Mechanism:** Acquires an exclusive row-level lock on the PostgreSQL record at the start of the transaction.
   - **Pros:** Strong consistency; subsequent transactions block until the lock releases.
   - **Cons:** High lock contention, risk of deadlocks across distributed workflows, and holds database connections open waiting for locks.
2. **Optimistic Locking with JPA `@Version` (Chosen):**
   - **Mechanism:** A `version BIGINT` column is verified during `UPDATE`:
     ```sql
     UPDATE orders SET status = ?, version = version + 1 WHERE id = ? AND version = ?;
     ```
   - **Pros:** Zero row locks held during application processing; high throughput.
   - **Behavior on Collision:** If another thread updated the order first, 0 rows are updated, and Hibernate immediately raises `OptimisticLockingFailureException`, which we translate into an HTTP 409 Conflict with compensation guidance.

### Pricing Snapshot
- **Decision:** Copy `price_cents` into `order_items` at order placement time.
- **Why:** Protects financial records from retrospective restaurant menu edits.

## Decision
We enforce a strict **Domain-Driven Order State Machine** embedded in the `Order` entity and use **Optimistic Locking (`@Version`)** for all state updates.

## Consequences
### Positive
- Strict forward-only state progression guaranteed at both domain model and database constraint levels.
- Non-blocking concurrent throughput across high-frequency order operations.
- Historical billing integrity is preserved via point-in-time price snapshots.

### Negative / Trade-offs
- Callers experiencing an optimistic lock failure must handle HTTP 409 and retry with the updated state.

## Code and Config Affected
- `services/order-service/src/main/resources/db/migration/V1__init_order_schema.sql` (column: `version BIGINT`)
- `services/order-service/src/main/java/com/swiggy/order/model/OrderStatus.java`
- `services/order-service/src/main/java/com/swiggy/order/model/Order.java`
- `services/order-service/src/main/java/com/swiggy/order/service/OrderService.java`
