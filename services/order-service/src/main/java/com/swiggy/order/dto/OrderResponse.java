package com.swiggy.order.dto;

import com.swiggy.order.model.Order;
import com.swiggy.order.model.OrderStatus;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public record OrderResponse(
        UUID id,
        UUID customerId,
        UUID restaurantId,
        UUID riderId,
        OrderStatus status,
        int totalAmountCents,
        String deliveryAddress,
        Long version,
        List<OrderItemResponse> items,
        Instant createdAt
) {
    public static OrderResponse fromEntity(Order order) {
        List<OrderItemResponse> itemResponses = order.getItems() != null
                ? order.getItems().stream().map(OrderItemResponse::fromEntity).toList()
                : Collections.emptyList();

        return new OrderResponse(
                order.getId(),
                order.getCustomerId(),
                order.getRestaurantId(),
                order.getRiderId(),
                order.getStatus(),
                order.getTotalAmountCents(),
                order.getDeliveryAddress(),
                order.getVersion(),
                itemResponses,
                order.getCreatedAt()
        );
    }
}
