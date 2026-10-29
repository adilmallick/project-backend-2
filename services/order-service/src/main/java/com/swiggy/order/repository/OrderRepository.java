package com.swiggy.order.repository;

import com.swiggy.order.model.Order;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA Repository for Order aggregate root.
 *
 * <p>SEE: ADR-005 (Order State Machine & Optimistic Locking)
 */
@Repository
public interface OrderRepository extends JpaRepository<Order, UUID> {

    // DECISION: Uses @EntityGraph to eagerly fetch order items in 1 query when retrieving order details.
    @EntityGraph(attributePaths = {"items"})
    @Query("SELECT o FROM Order o WHERE o.id = :id")
    Optional<Order> findByIdWithItems(UUID id);

    List<Order> findByCustomerIdOrderByCreatedAtDesc(UUID customerId);

    List<Order> findByRestaurantIdOrderByCreatedAtDesc(UUID restaurantId);
}
