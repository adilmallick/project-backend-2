package com.swiggy.order.event;

import java.util.UUID;

/**
 * Event published by Payment Service or simulator indicating payment authorization outcome.
 *
 * <p>SEE: ADR-008 (Transactional Outbox Pattern & Event-Driven Sagas)
 */
public record PaymentEvent(
        UUID orderId,
        boolean success,
        String failureReason
) {}
