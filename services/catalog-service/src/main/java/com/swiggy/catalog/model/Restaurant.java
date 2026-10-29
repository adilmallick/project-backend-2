package com.swiggy.catalog.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Restaurant Merchant Profile Entity.
 *
 * <p>DECISION: Maps to Flyway 'restaurants' table in catalog_db.
 *
 * <p>DECISION: Uses bidirectional @OneToMany relationship to MenuItem with FetchType.LAZY.
 * INVARIANT: Never use FetchType.EAGER on collection mappings to avoid memory bloat and unintended queries.
 *
 * <p>SEE: ADR-004 (Query Optimization and N+1 Prevention)
 */
@Entity
@Table(name = "restaurants")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Restaurant {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "address", nullable = false, length = 255)
    private String address;

    // DECISION: Logical reference to merchant user ID from user-service without a DB foreign key.
    // INVARIANT: Cross-database foreign keys are physically forbidden in microservices.
    // SEE: ADR-001 (Database-per-Service)
    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    // DECISION: CascadeType.ALL ensures saving a restaurant persists its menu items.
    // orphanRemoval = true ensures removed items are deleted from database.
    @Builder.Default
    @OneToMany(mappedBy = "restaurant", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<MenuItem> menuItems = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    // DECISION: Synchronize both sides of the bidirectional association to avoid transient reference issues.
    public void addMenuItem(MenuItem item) {
        menuItems.add(item);
        item.setRestaurant(this);
    }

    public void removeMenuItem(MenuItem item) {
        menuItems.remove(item);
        item.setRestaurant(null);
    }
}
