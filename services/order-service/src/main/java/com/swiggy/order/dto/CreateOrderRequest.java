package com.swiggy.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public record CreateOrderRequest(
        @NotNull(message = "Customer ID is required")
        UUID customerId,

        @NotNull(message = "Restaurant ID is required")
        UUID restaurantId,

        @NotBlank(message = "Delivery address is required")
        @Size(max = 255, message = "Delivery address cannot exceed 255 characters")
        String deliveryAddress,

        @NotEmpty(message = "Order must contain at least one item")
        @Valid
        List<OrderItemRequest> items
) {}
