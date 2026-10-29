package com.swiggy.catalog.dto;

import com.swiggy.catalog.model.MenuItem;

import java.util.UUID;

/**
 * Response DTO for menu items.
 */
public record MenuItemResponse(
        UUID id,
        String name,
        String description,
        int priceCents,
        boolean isAvailable
) {
    public static MenuItemResponse fromEntity(MenuItem item) {
        return new MenuItemResponse(
                item.getId(),
                item.getName(),
                item.getDescription(),
                item.getPriceCents(),
                item.isAvailable()
        );
    }
}
