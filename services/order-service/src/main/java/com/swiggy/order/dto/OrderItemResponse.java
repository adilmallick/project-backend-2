package com.swiggy.order.dto;

import com.swiggy.order.model.OrderItem;

import java.util.UUID;

public record OrderItemResponse(
        UUID id,
        UUID menuItemId,
        String itemName,
        int quantity,
        int priceCents
) {
    public static OrderItemResponse fromEntity(OrderItem item) {
        return new OrderItemResponse(
                item.getId(),
                item.getMenuItemId(),
                item.getItemName(),
                item.getQuantity(),
                item.getPriceCents()
        );
    }
}
