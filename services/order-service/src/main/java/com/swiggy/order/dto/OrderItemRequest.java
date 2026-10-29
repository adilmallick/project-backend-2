package com.swiggy.order.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record OrderItemRequest(
        @NotNull(message = "Menu item ID is required")
        UUID menuItemId,

        @NotBlank(message = "Item name is required")
        String itemName,

        @Min(value = 1, message = "Quantity must be at least 1")
        int quantity,

        @Min(value = 0, message = "Price cannot be negative")
        int priceCents
) {}
