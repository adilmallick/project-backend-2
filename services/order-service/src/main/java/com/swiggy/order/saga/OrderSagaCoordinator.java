package com.swiggy.order.saga;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.swiggy.order.event.PaymentEvent;
import com.swiggy.order.model.OrderStatus;
import com.swiggy.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

/**
 * Saga Coordinator managing order state progression and compensating transactions.
 *
 * <p>Listens to payment.events:
 * - On success: transitions order forward to RESTAURANT_ACCEPTED.
 * - On failure: executes compensating transaction, transitioning order to CANCELLED.
 *
 * <p>SEE: ADR-008 (Transactional Outbox Pattern & Event-Driven Sagas)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderSagaCoordinator {

    private final OrderService orderService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "payment.events", groupId = "order-saga-coordinator-group")
    public void onPaymentEvent(String message) {
        try {
            PaymentEvent event = objectMapper.readValue(message, PaymentEvent.class);
            log.info("OrderSagaCoordinator received PaymentEvent for orderId={}, success={}",
                    event.orderId(), event.success());

            if (event.success()) {
                log.info("Payment succeeded. Transitioning orderId={} to RESTAURANT_ACCEPTED", event.orderId());
                orderService.updateOrderStatus(event.orderId(), OrderStatus.RESTAURANT_ACCEPTED);
            } else {
                // COMPENSATING TRANSACTION:
                // Roll back / cancel the order since the customer was not charged.
                log.warn("Payment FAILED for orderId={}. Executing COMPENSATING TRANSACTION: CANCELLED order. Reason: {}",
                        event.orderId(), event.failureReason());
                orderService.updateOrderStatus(event.orderId(), OrderStatus.CANCELLED);
            }
        } catch (Exception ex) {
            log.error("Failed to coordinate order saga for message: {}", message, ex);
        }
    }
}
