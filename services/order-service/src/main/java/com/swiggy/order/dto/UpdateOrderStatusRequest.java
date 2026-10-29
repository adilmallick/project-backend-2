package com.swiggy.order.dto;

import com.swiggy.order.model.OrderStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateOrderStatusRequest(
        @NotNull(message = "New order status is required")
        OrderStatus status
) {}
