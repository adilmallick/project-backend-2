package com.swiggy.order.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/**
 * Order Item Entity representing line items captured at order placement time.
 *
 * <p>DECISION: Captures point-in-time snapshot of item name and price in cents.
 *
 * <p>SEE: ADR-005
 */
@Entity
@Table(name = "order_items")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(name = "menu_item_id", nullable = false)
    private UUID menuItemId;

    @Column(name = "item_name", nullable = false, length = 150)
    private String itemName;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    @Column(name = "price_cents", nullable = false)
    private int priceCents;
}
