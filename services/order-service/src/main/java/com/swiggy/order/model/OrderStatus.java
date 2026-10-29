package com.swiggy.order.model;

import java.util.Set;

/**
 * Order Lifecycle State Machine enumeration.
 *
 * <p>DECISION: Encapsulates state transition rules directly within the domain enum.
 *
 * <p>INVARIANT: State transitions only move forward along defined edges.
 * Once an order reaches PICKED_UP, it cannot be CANCELLED by the customer.
 * DELIVERED and REFUNDED are terminal states.
 *
 * <p>SEE: ADR-005
 */
public enum OrderStatus {
    ORDER_PLACED,
    RESTAURANT_ACCEPTED,
    RIDER_ASSIGNED,
    PICKED_UP,
    DELIVERED,
    CANCELLED,
    REFUNDED;

    public boolean canTransitionTo(OrderStatus next) {
        if (next == null) {
            return false;
        }

        return switch (this) {
            case ORDER_PLACED -> next == RESTAURANT_ACCEPTED || next == CANCELLED;
            case RESTAURANT_ACCEPTED -> next == RIDER_ASSIGNED || next == CANCELLED;
            case RIDER_ASSIGNED -> next == PICKED_UP || next == CANCELLED;
            case PICKED_UP -> next == DELIVERED;
            case CANCELLED -> next == REFUNDED;
            case DELIVERED, REFUNDED -> false; // Terminal states
        };
    }
}
