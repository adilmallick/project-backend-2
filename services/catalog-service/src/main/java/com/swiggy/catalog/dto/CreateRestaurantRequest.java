package com.swiggy.catalog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * Request DTO for creating a new restaurant profile.
 */
public record CreateRestaurantRequest(
        @NotBlank(message = "Restaurant name is required")
        @Size(max = 150, message = "Name cannot exceed 150 characters")
        String name,

        String description,

        @NotBlank(message = "Address is required")
        @Size(max = 255, message = "Address cannot exceed 255 characters")
        String address,

        @NotNull(message = "Owner ID is required")
        UUID ownerId
) {}
