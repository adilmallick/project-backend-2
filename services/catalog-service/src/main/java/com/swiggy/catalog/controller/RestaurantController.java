package com.swiggy.catalog.controller;

import com.swiggy.catalog.dto.*;
import com.swiggy.catalog.service.RestaurantService;
import com.swiggy.common.dto.ApiResponse;
import com.swiggy.common.filter.CorrelationIdUtils;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Controller exposing restaurant and catalog management APIs.
 *
 * <p>SEE: ADR-004 (JPA Query Optimization and N+1 Prevention)
 */
@RestController
@RequestMapping("/api/v1/restaurants")
public class RestaurantController {

    private final RestaurantService restaurantService;

    public RestaurantController(RestaurantService restaurantService) {
        this.restaurantService = restaurantService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<RestaurantResponse>> createRestaurant(
            @Valid @RequestBody CreateRestaurantRequest request
    ) {
        RestaurantResponse response = restaurantService.createRestaurant(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, CorrelationIdUtils.getCorrelationId()));
    }

    @PostMapping("/{id}/menu-items")
    public ResponseEntity<ApiResponse<MenuItemResponse>> addMenuItem(
            @PathVariable UUID id,
            @Valid @RequestBody CreateMenuItemRequest request
    ) {
        MenuItemResponse response = restaurantService.addMenuItem(id, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, CorrelationIdUtils.getCorrelationId()));
    }

    // =========================================================================
    // 1. UNOPTIMIZED DEMONSTRATION ENDPOINT (Triggers N+1 Queries)
    // =========================================================================
    @GetMapping("/unoptimized")
    public ResponseEntity<ApiResponse<List<RestaurantResponse>>> getRestaurantsUnoptimized() {
        List<RestaurantResponse> response = restaurantService.getRestaurantsUnoptimized();
        return ResponseEntity.ok(ApiResponse.success(response, CorrelationIdUtils.getCorrelationId()));
    }

    // =========================================================================
    // 2. PRODUCTION OPTIMIZED ENDPOINT (Single Query via @EntityGraph)
    // =========================================================================
    @GetMapping
    public ResponseEntity<ApiResponse<List<RestaurantResponse>>> getRestaurantsOptimized() {
        List<RestaurantResponse> response = restaurantService.getRestaurantsOptimized();
        return ResponseEntity.ok(ApiResponse.success(response, CorrelationIdUtils.getCorrelationId()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<RestaurantResponse>> getRestaurantById(@PathVariable UUID id) {
        RestaurantResponse response = restaurantService.getRestaurantById(id);
        return ResponseEntity.ok(ApiResponse.success(response, CorrelationIdUtils.getCorrelationId()));
    }

    // =========================================================================
    // 3. PAGINATED SEARCH ENDPOINT
    // =========================================================================
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<Page<RestaurantSummaryResponse>>> searchRestaurants(
            @RequestParam(required = false) String name,
            @PageableDefault(size = 10, sort = "name", direction = Sort.Direction.ASC) Pageable pageable
    ) {
        Page<RestaurantSummaryResponse> response = restaurantService.searchRestaurants(name, pageable);
        return ResponseEntity.ok(ApiResponse.success(response, CorrelationIdUtils.getCorrelationId()));
    }
}
