package com.swiggy.catalog.service;

import com.swiggy.catalog.dto.*;
import com.swiggy.catalog.model.MenuItem;
import com.swiggy.catalog.model.Restaurant;
import com.swiggy.catalog.repository.MenuItemRepository;
import com.swiggy.catalog.repository.RestaurantRepository;
import com.swiggy.common.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Service orchestrating restaurant catalog management and query execution.
 *
 * <p>SEE: ADR-004 (JPA Query Optimization and N+1 Prevention)
 */
@Service
public class RestaurantService {

    private static final Logger log = LoggerFactory.getLogger(RestaurantService.class);

    private final RestaurantRepository restaurantRepository;
    private final MenuItemRepository menuItemRepository;

    public RestaurantService(RestaurantRepository restaurantRepository,
                             MenuItemRepository menuItemRepository) {
        this.restaurantRepository = restaurantRepository;
        this.menuItemRepository = menuItemRepository;
    }

    @Transactional
    public RestaurantResponse createRestaurant(CreateRestaurantRequest request) {
        Restaurant restaurant = Restaurant.builder()
                .name(request.name().trim())
                .description(request.description())
                .address(request.address().trim())
                .ownerId(request.ownerId())
                .isActive(true)
                .build();

        Restaurant saved = restaurantRepository.save(restaurant);
        return RestaurantResponse.fromEntity(saved);
    }

    @Transactional
    public MenuItemResponse addMenuItem(UUID restaurantId, CreateMenuItemRequest request) {
        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant", restaurantId));

        MenuItem item = MenuItem.builder()
                .restaurant(restaurant)
                .name(request.name().trim())
                .description(request.description())
                .priceCents(request.priceCents())
                .isAvailable(true)
                .build();

        restaurant.addMenuItem(item);
        MenuItem saved = menuItemRepository.save(item);

        return MenuItemResponse.fromEntity(saved);
    }

    // =========================================================================
    // 1. UNOPTIMIZED FETCH (Demonstration of N+1 Query Anti-Pattern)
    // =========================================================================
    // DECISION: Deliberately queries restaurants without fetching menuItems.
    // Iterating through getMenuItems() causes Hibernate to issue 1 additional query for each restaurant.
    @Transactional(readOnly = true)
    public List<RestaurantResponse> getRestaurantsUnoptimized() {
        log.warn("Executing UNOPTIMIZED restaurant query: this will trigger N+1 separate SQL round-trips!");
        List<Restaurant> restaurants = restaurantRepository.findByIsActiveTrue();

        // Iteration forces lazy collection initialization -> fires 1 SQL query per restaurant
        return restaurants.stream()
                .map(RestaurantResponse::fromEntity)
                .toList();
    }

    // =========================================================================
    // 2. PRODUCTION OPTIMIZED FETCH (@EntityGraph / Exactly 1 SQL Query)
    // =========================================================================
    // DECISION: Uses @EntityGraph to load both restaurants and their menu items in a single SQL query.
    // SEE: ADR-004
    @Transactional(readOnly = true)
    public List<RestaurantResponse> getRestaurantsOptimized() {
        log.info("Executing PRODUCTION OPTIMIZED query with @EntityGraph (Single SQL round-trip)");
        List<Restaurant> restaurants = restaurantRepository.findAllWithMenuItemsGraph();

        return restaurants.stream()
                .map(RestaurantResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public RestaurantResponse getRestaurantById(UUID id) {
        Restaurant restaurant = restaurantRepository.findByIdWithMenuItems(id)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant", id));
        return RestaurantResponse.fromEntity(restaurant);
    }

    // =========================================================================
    // 3. PAGINATED SEARCH
    // =========================================================================
    // DECISION: Uses Spring Data Pageable to limit database record extraction and query execution.
    @Transactional(readOnly = true)
    public Page<RestaurantSummaryResponse> searchRestaurants(String name, Pageable pageable) {
        String searchTerm = name != null ? name.trim() : "";
        return restaurantRepository.findByIsActiveTrueAndNameContainingIgnoreCase(searchTerm, pageable)
                .map(RestaurantSummaryResponse::fromEntity);
    }
}
