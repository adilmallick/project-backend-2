-- =============================================================================
-- V1__init_order_schema.sql
-- Baseline Flyway migration for order-service (order_db)
-- =============================================================================

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- DECISION: Orders table encapsulates the core order entity and saga lifecycle.
-- INVARIANT: Order status transitions must strictly follow the saga state machine:
-- ORDER_PLACED -> PAYMENT_COMPLETED -> RESTAURANT_ACCEPTED -> RIDER_ASSIGNED -> PICKED_UP -> DELIVERED
-- Failure states: CANCELLED, REFUNDED.
CREATE TABLE IF NOT EXISTS orders (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),

    -- DECISION: Store customer_id and restaurant_id as UUID foreign keys from other services without DB-level constraints.
    -- INVARIANT: In microservices, services do not have foreign key constraints across database boundaries.
    -- SEE: ADR-001 (Database-per-Service)
    customer_id UUID NOT NULL,
    restaurant_id UUID NOT NULL,
    rider_id UUID,

    -- DECISION: Status string with check constraint enforces valid lifecycle states at the database level.
    status VARCHAR(50) NOT NULL CHECK (
        status IN (
            'ORDER_PLACED',
            'PAYMENT_COMPLETED',
            'RESTAURANT_ACCEPTED',
            'RIDER_ASSIGNED',
            'PICKED_UP',
            'DELIVERED',
            'CANCELLED',
            'REFUNDED'
        )
    ),

    -- DECISION: Total order amount stored as integer cents to avoid floating point precision issues.
    total_amount_cents INTEGER NOT NULL CHECK (total_amount_cents >= 0),

    delivery_address VARCHAR(255) NOT NULL,

    -- DECISION: Version column supports JPA Optimistic Locking (@Version) to prevent lost updates under concurrent modifications.
    -- SEE: Chaos Exercise (Stage 1)
    version BIGINT NOT NULL DEFAULT 0,

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- DECISION: Indexes for primary query paths (customer order history, status filtering).
CREATE INDEX IF NOT EXISTS idx_orders_customer_id ON orders(customer_id);
CREATE INDEX IF NOT EXISTS idx_orders_status ON orders(status);
CREATE INDEX IF NOT EXISTS idx_orders_restaurant_id ON orders(restaurant_id);

-- DECISION: Order items line items table stores point-in-time snapshot of purchased items.
CREATE TABLE IF NOT EXISTS order_items (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    order_id UUID NOT NULL,
    menu_item_id UUID NOT NULL,
    item_name VARCHAR(150) NOT NULL,
    quantity INTEGER NOT NULL CHECK (quantity > 0),
    price_cents INTEGER NOT NULL CHECK (price_cents >= 0),

    -- DECISION: Local foreign key ensures line items cascade when order is deleted.
    CONSTRAINT fk_order_items_order FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_order_items_order_id ON order_items(order_id);
