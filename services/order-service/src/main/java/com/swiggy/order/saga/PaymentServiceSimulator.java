package com.swiggy.order.saga;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.swiggy.order.event.OrderPlacedEvent;
import com.swiggy.order.event.PaymentEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

/**
 * Service simulating an external asynchronous Payment Gateway / Service.
 *
 * <p>Demonstrates event-driven choreographies and Saga compensation:
 * - Orders over ₹1,000 (100,000 cents) trigger simulated payment failure to test compensation.
 * - Standard orders trigger payment success.
 *
 * <p>SEE: ADR-008 (Transactional Outbox Pattern & Event-Driven Sagas)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceSimulator {

    public static final String PAYMENT_EVENTS_TOPIC = "payment.events";
    public static final long PAYMENT_FAILURE_THRESHOLD_CENTS = 100_000L; // ₹1,000

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "order.events", groupId = "payment-simulator-group")
    public void onOrderPlaced(String message) {
        try {
            OrderPlacedEvent event = objectMapper.readValue(message, OrderPlacedEvent.class);
            log.info("PaymentSimulator received OrderPlacedEvent for orderId={}, amountCents={}",
                    event.orderId(), event.totalAmountCents());

            boolean success = event.totalAmountCents() < PAYMENT_FAILURE_THRESHOLD_CENTS;
            String reason = success ? null : "INSUFFICIENT_FUNDS_OR_CARD_DECLINED";

            PaymentEvent paymentEvent = new PaymentEvent(event.orderId(), success, reason);
            String payload = objectMapper.writeValueAsString(paymentEvent);

            kafkaTemplate.send(PAYMENT_EVENTS_TOPIC, event.orderId().toString(), payload);
            log.info("PaymentSimulator published PaymentEvent for orderId={}, success={}",
                    event.orderId(), success);
        } catch (Exception ex) {
            log.error("Failed to process payment for message: {}", message, ex);
        }
    }
}
