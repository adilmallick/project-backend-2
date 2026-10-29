package com.swiggy.order.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Domain event published when an order is first accepted and committed to order_db.
 *
 * <p>SEE: ADR-008 (Transactional Outbox Pattern & Event-Driven Sagas)
 */
public record OrderPlacedEvent(
        UUID orderId,
        UUID customerId,
        UUID restaurantId,
        long totalAmountCents,
        Instant createdAt
) {}
