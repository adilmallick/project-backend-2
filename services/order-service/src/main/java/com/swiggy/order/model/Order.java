package com.swiggy.order.model;

import com.swiggy.common.exception.BusinessException;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Order Aggregate Root Entity.
 *
 * <p>DECISION: Maps to Flyway 'orders' table in order_db.
 *
 * <p>DECISION: Uses JPA @Version column for non-blocking Optimistic Concurrency Control.
 * When concurrent requests update the order, Hibernate ensures only the first committer succeeds,
 * throwing OptimisticLockException on stale versions.
 *
 * <p>INVARIANT: Status transitions must obey {@link OrderStatus#canTransitionTo(OrderStatus)}.
 *
 * <p>SEE: ADR-001 (Database-per-Service)
 * <p>SEE: ADR-005 (Order State Machine & Optimistic Locking)
 */
@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    // DECISION: Logical cross-service IDs without database foreign keys.
    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Column(name = "restaurant_id", nullable = false)
    private UUID restaurantId;

    @Column(name = "rider_id")
    private UUID riderId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private OrderStatus status;

    @Column(name = "total_amount_cents", nullable = false)
    private int totalAmountCents;

    @Column(name = "delivery_address", nullable = false, length = 255)
    private String deliveryAddress;

    // DECISION: Optimistic locking version number incremented automatically by Hibernate on update.
    // SEE: ADR-005
    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Builder.Default
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<OrderItem> items = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public void addItem(OrderItem item) {
        items.add(item);
        item.setOrder(this);
    }

    public void calculateTotal() {
        this.totalAmountCents = items.stream()
                .mapToInt(item -> item.getPriceCents() * item.getQuantity())
                .sum();
    }

    public void transitionTo(OrderStatus newStatus) {
        if (!this.status.canTransitionTo(newStatus)) {
            throw new BusinessException(
                    "INVALID_STATUS_TRANSITION",
                    String.format("Cannot transition order from status '%s' to '%s'", this.status, newStatus),
                    HttpStatus.CONFLICT
            );
        }
        this.status = newStatus;
    }
}
