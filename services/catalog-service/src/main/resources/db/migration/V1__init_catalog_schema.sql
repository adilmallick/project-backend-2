-- =============================================================================
-- V1__init_catalog_schema.sql
-- Baseline Flyway migration for catalog-service (catalog_db)
-- =============================================================================

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- DECISION: Restaurants table stores restaurant merchant profiles.
CREATE TABLE IF NOT EXISTS restaurants (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(150) NOT NULL,
    description TEXT,
    address VARCHAR(255) NOT NULL,
    -- DECISION: Owner ID stores UUID of the merchant from user-service without a DB foreign key.
    -- INVARIANT: Cross-service data integrity is enforced logically via APIs/events, not SQL foreign keys.
    -- SEE: ADR-001 (Database-per-Service)
    owner_id UUID NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_restaurants_owner_id ON restaurants(owner_id);
CREATE INDEX IF NOT EXISTS idx_restaurants_is_active ON restaurants(is_active);

-- DECISION: Menu items table stores catalog items sold by restaurants.
CREATE TABLE IF NOT EXISTS menu_items (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    restaurant_id UUID NOT NULL,
    name VARCHAR(150) NOT NULL,
    description TEXT,

    -- DECISION: Store currency amounts in smallest monetary unit (cents/paise) as BIGINT/INTEGER.
    -- ALTERNATIVES: FLOAT or DOUBLE (rejected: IEEE 754 floating point arithmetic introduces rounding errors).
    -- ALTERNATIVES: NUMERIC/DECIMAL (viable, but integer cents is faster and eliminates rounding ambiguities).
    price_cents INTEGER NOT NULL CHECK (price_cents >= 0),

    is_available BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- DECISION: Local foreign key within catalog_db ensures menu items are deleted if restaurant is removed.
    CONSTRAINT fk_menu_items_restaurant FOREIGN KEY (restaurant_id) REFERENCES restaurants(id) ON DELETE CASCADE
);

-- DECISION: Foreign key index on restaurant_id prevents full-table scans during menu lookups and cascade operations.
CREATE INDEX IF NOT EXISTS idx_menu_items_restaurant_id ON menu_items(restaurant_id);
