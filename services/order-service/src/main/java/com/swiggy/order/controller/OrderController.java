package com.swiggy.order.controller;

import com.swiggy.common.dto.ApiResponse;
import com.swiggy.common.filter.CorrelationIdUtils;
import com.swiggy.order.dto.CreateOrderRequest;
import com.swiggy.order.dto.OrderResponse;
import com.swiggy.order.dto.UpdateOrderStatusRequest;
import com.swiggy.order.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Controller exposing order creation and lifecycle transition APIs.
 *
 * <p>SEE: ADR-005 (Order State Machine & Optimistic Locking)
 */
@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<OrderResponse>> createOrder(
            @Valid @RequestBody CreateOrderRequest request
    ) {
        OrderResponse response = orderService.createOrder(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, CorrelationIdUtils.getCorrelationId()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrderById(@PathVariable UUID id) {
        OrderResponse response = orderService.getOrderById(id);
        return ResponseEntity.ok(ApiResponse.success(response, CorrelationIdUtils.getCorrelationId()));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<OrderResponse>> updateOrderStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateOrderStatusRequest request
    ) {
        OrderResponse response = orderService.updateOrderStatus(id, request.status());
        return ResponseEntity.ok(ApiResponse.success(response, CorrelationIdUtils.getCorrelationId()));
    }

    @GetMapping("/customer/{customerId}")
    public ResponseEntity<ApiResponse<List<OrderResponse>>> getOrdersByCustomer(
            @PathVariable UUID customerId
    ) {
        List<OrderResponse> response = orderService.getOrdersByCustomer(customerId);
        return ResponseEntity.ok(ApiResponse.success(response, CorrelationIdUtils.getCorrelationId()));
    }
}
