package com.swiggy.catalog.repository;

import com.swiggy.catalog.model.Restaurant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA Repository for Restaurant entities.
 *
 * <p>SEE: ADR-004 (JPA Query Optimization and N+1 Prevention)
 */
@Repository
public interface RestaurantRepository extends JpaRepository<Restaurant, UUID> {

    // =========================================================================
    // 1. UNOPTIMIZED QUERY (Demonstrates the N+1 Query Disaster)
    // =========================================================================
    // DECISION: Intentionally simple query that does NOT fetch menuItems.
    // When code accesses restaurant.getMenuItems(), Hibernate generates 1 additional SQL query per restaurant.
    // For N restaurants, this produces 1 + N queries.
    List<Restaurant> findByIsActiveTrue();

    // =========================================================================
    // 2. PRODUCTION OPTIMIZED QUERY (@EntityGraph / Single SQL Query)
    // =========================================================================
    // DECISION: Uses @EntityGraph to instruct Hibernate to generate a single SQL query
    // with a 'LEFT OUTER JOIN menu_items' clause, reducing database round-trips from N+1 to exactly 1.
    // SEE: ADR-004
    @EntityGraph(attributePaths = {"menuItems"})
    @Query("SELECT DISTINCT r FROM Restaurant r WHERE r.isActive = true")
    List<Restaurant> findAllWithMenuItemsGraph();

    // =========================================================================
    // 3. SINGLE RESTAURANT LOOKUP WITH FULL MENU GRAPH
    // =========================================================================
    @EntityGraph(attributePaths = {"menuItems"})
    @Query("SELECT r FROM Restaurant r WHERE r.id = :id AND r.isActive = true")
    Optional<Restaurant> findByIdWithMenuItems(UUID id);

    // =========================================================================
    // 4. PAGINATED SEARCH QUERY (With Database Index Optimization)
    // =========================================================================
    // DECISION: Returns Spring Data Page<Restaurant> with total element count and pagination slice.
    // Does not fetch menuItems because search result pages only display restaurant metadata.
    Page<Restaurant> findByIsActiveTrueAndNameContainingIgnoreCase(String name, Pageable pageable);
}
