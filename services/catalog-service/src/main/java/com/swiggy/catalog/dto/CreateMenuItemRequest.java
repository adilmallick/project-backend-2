package com.swiggy.catalog.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for adding a menu item to a restaurant.
 */
public record CreateMenuItemRequest(
        @NotBlank(message = "Item name is required")
        @Size(max = 150, message = "Name cannot exceed 150 characters")
        String name,

        String description,

        @Min(value = 0, message = "Price must be greater than or equal to 0")
        int priceCents
) {}
