package com.swiggy.catalog.dto;

import com.swiggy.catalog.model.Restaurant;

import java.util.UUID;

/**
 * Lightweight Restaurant summary response for paginated search results without loading full menus.
 */
public record RestaurantSummaryResponse(
        UUID id,
        String name,
        String description,
        String address,
        boolean isActive
) {
    public static RestaurantSummaryResponse fromEntity(Restaurant restaurant) {
        return new RestaurantSummaryResponse(
                restaurant.getId(),
                restaurant.getName(),
                restaurant.getDescription(),
                restaurant.getAddress(),
                restaurant.isActive()
        );
    }
}
