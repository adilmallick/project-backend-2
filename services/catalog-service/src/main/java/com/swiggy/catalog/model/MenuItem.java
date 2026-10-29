package com.swiggy.catalog.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * Menu Item Entity representing food items offered by restaurants.
 *
 * <p>DECISION: Maps to Flyway 'menu_items' table in catalog_db.
 *
 * <p>INVARIANT: priceCents must be non-negative integer representing the smallest currency unit.
 *
 * <p>SEE: ADR-004
 */
@Entity
@Table(name = "menu_items")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class MenuItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    // DECISION: ManyToOne relationship with FetchType.LAZY avoids loading full Restaurant on item queries.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "restaurant_id", nullable = false)
    private Restaurant restaurant;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    // DECISION: Store currency in smallest denomination (cents/paise) as integer.
    // ALTERNATIVES: float/double (rejected: IEEE 754 precision errors in billing).
    @Column(name = "price_cents", nullable = false)
    private int priceCents;

    @Builder.Default
    @Column(name = "is_available", nullable = false)
    private boolean isAvailable = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
