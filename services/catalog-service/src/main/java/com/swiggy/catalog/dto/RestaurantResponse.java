package com.swiggy.catalog.dto;

import com.swiggy.catalog.model.Restaurant;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Detailed Restaurant response containing the full menu items graph.
 */
public record RestaurantResponse(
        UUID id,
        String name,
        String description,
        String address,
        UUID ownerId,
        boolean isActive,
        List<MenuItemResponse> menuItems
) {
    public static RestaurantResponse fromEntity(Restaurant restaurant) {
        List<MenuItemResponse> items = restaurant.getMenuItems() != null
                ? restaurant.getMenuItems().stream().map(MenuItemResponse::fromEntity).toList()
                : Collections.emptyList();

        return new RestaurantResponse(
                restaurant.getId(),
                restaurant.getName(),
                restaurant.getDescription(),
                restaurant.getAddress(),
                restaurant.getOwnerId(),
                restaurant.isActive(),
                items
        );
    }
}
