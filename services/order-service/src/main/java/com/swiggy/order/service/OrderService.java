package com.swiggy.order.service;

import com.swiggy.common.exception.ResourceNotFoundException;
import com.swiggy.order.dto.CreateOrderRequest;
import com.swiggy.order.dto.OrderResponse;
import com.swiggy.order.model.Order;
import com.swiggy.order.model.OrderItem;
import com.swiggy.order.model.OrderStatus;
import com.swiggy.order.repository.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Service orchestrating order lifecycle, state machine transitions, and optimistic concurrency.
 *
 * <p>SEE: ADR-005 (Order State Machine & Optimistic Locking)
 */
@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orderRepository;

    public OrderService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request) {
        log.info("Creating new order for customerId={} at restaurantId={}",
                request.customerId(), request.restaurantId());

        Order order = Order.builder()
                .customerId(request.customerId())
                .restaurantId(request.restaurantId())
                .deliveryAddress(request.deliveryAddress().trim())
                .status(OrderStatus.ORDER_PLACED)
                .build();

        // DECISION: Capture point-in-time snapshot of purchased items and their prices.
        // INVARIANT: Historical order line items remain unaffected by future catalog price edits.
        // SEE: ADR-005
        request.items().forEach(itemReq -> {
            OrderItem item = OrderItem.builder()
                    .menuItemId(itemReq.menuItemId())
                    .itemName(itemReq.itemName().trim())
                    .quantity(itemReq.quantity())
                    .priceCents(itemReq.priceCents())
                    .build();
            order.addItem(item);
        });

        order.calculateTotal();
        Order saved = orderRepository.save(order);

        log.info("Order created successfully: id={}, totalCents={}, status={}",
                saved.getId(), saved.getTotalAmountCents(), saved.getStatus());

        return OrderResponse.fromEntity(saved);
    }

    @Transactional
    public OrderResponse updateOrderStatus(UUID orderId, OrderStatus newStatus) {
        log.info("Transitioning order id={} to newStatus={}", orderId, newStatus);

        Order order = orderRepository.findByIdWithItems(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));

        // DECISION: Enforces state machine invariants in domain model.
        // Throws BusinessException("INVALID_STATUS_TRANSITION") if illegal.
        order.transitionTo(newStatus);

        // DECISION: saveAndFlush forces immediate SQL flush so Hibernate increments @Version
        // on the managed entity and verifies optimistic lock before returning the response DTO.
        Order updated = orderRepository.saveAndFlush(order);

        return OrderResponse.fromEntity(updated);
    }

    @Transactional
    public OrderResponse assignRider(UUID orderId, UUID riderId) {
        Order order = orderRepository.findByIdWithItems(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));

        order.transitionTo(OrderStatus.RIDER_ASSIGNED);
        order.setRiderId(riderId);

        Order updated = orderRepository.saveAndFlush(order);
        return OrderResponse.fromEntity(updated);
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrderById(UUID id) {
        Order order = orderRepository.findByIdWithItems(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order", id));
        return OrderResponse.fromEntity(order);
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getOrdersByCustomer(UUID customerId) {
        return orderRepository.findByCustomerIdOrderByCreatedAtDesc(customerId).stream()
                .map(OrderResponse::fromEntity)
                .toList();
    }
}
