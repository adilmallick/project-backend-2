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

import com.swiggy.common.exception.BusinessException;
import com.swiggy.order.idempotency.IdempotencyService;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Controller exposing order creation and lifecycle transition APIs.
 *
 * <p>SEE: ADR-005 (Order State Machine & Optimistic Locking)
 * <p>SEE: ADR-007 (Distributed Rate Limiting & Idempotency using Redis)
 */
@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    private final OrderService orderService;
    private final IdempotencyService idempotencyService;

    public OrderController(OrderService orderService, IdempotencyService idempotencyService) {
        this.orderService = orderService;
        this.idempotencyService = idempotencyService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<OrderResponse>> createOrder(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody CreateOrderRequest request
    ) {
        // If idempotency key provided, check cache and acquire distributed lock
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            Optional<OrderResponse> cached = idempotencyService.getCachedResponse(idempotencyKey);
            if (cached.isPresent()) {
                return ResponseEntity.ok()
                        .header("X-Cache-Lookup", "HIT")
                        .body(ApiResponse.success(cached.get(), CorrelationIdUtils.getCorrelationId()));
            }

            boolean lockAcquired = idempotencyService.acquireLock(idempotencyKey, Duration.ofMinutes(2));
            if (!lockAcquired) {
                throw new BusinessException(
                        "CONCURRENT_REQUEST",
                        "Order creation is already in progress for idempotency key: " + idempotencyKey
                );
            }

            try {
                OrderResponse response = orderService.createOrder(request);
                idempotencyService.saveResponse(idempotencyKey, response, Duration.ofHours(24));
                return ResponseEntity.status(HttpStatus.CREATED)
                        .header("X-Cache-Lookup", "MISS")
                        .body(ApiResponse.success(response, CorrelationIdUtils.getCorrelationId()));
            } catch (RuntimeException ex) {
                idempotencyService.releaseLock(idempotencyKey);
                throw ex;
            }
        }

        // Standard creation without idempotency key
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
