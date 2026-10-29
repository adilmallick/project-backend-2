package com.swiggy.order.outbox;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Asynchronous Outbox Publisher relaying staged domain events to Apache Kafka.
 *
 * <p>DECISION: Uses aggregateId as Kafka message key to ensure all events for a given order
 * land on the exact same Kafka partition, guaranteeing strict FIFO event ordering per aggregate root.
 *
 * <p>DECISION: If Kafka is temporarily unreachable, events remain PENDING in the outbox table.
 * When Kafka recovers, the publisher drains the queue with zero data loss (Stage 3 Chaos requirement).
 *
 * <p>SEE: ADR-008 (Transactional Outbox Pattern & Event-Driven Sagas)
 */
@Slf4j
@Service
@EnableScheduling
@RequiredArgsConstructor
public class OutboxPublisherService {

    public static final String ORDER_EVENTS_TOPIC = "order.events";

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Scheduled(fixedDelayString = "${app.outbox.poll-interval-ms:1000}")
    @Transactional
    public void publishPendingEvents() {
        List<OutboxEvent> pendingEvents = outboxEventRepository.findPendingEvents();

        if (pendingEvents.isEmpty()) {
            return;
        }

        log.debug("Found {} pending outbox events to publish to Kafka.", pendingEvents.size());

        for (OutboxEvent event : pendingEvents) {
            String topic = resolveTopic(event.getAggregateType());
            String key = event.getAggregateId();

            try {
                // Send synchronously to guarantee Kafka ACK before committing outbox update
                kafkaTemplate.send(topic, key, event.getPayload()).get();

                event.setStatus(OutboxEvent.OutboxStatus.PUBLISHED);
                event.setPublishedAt(Instant.now());
                outboxEventRepository.save(event);

                log.info("Successfully published outbox event id={}, aggregateId={} to Kafka topic='{}'",
                        event.getId(), key, topic);
            } catch (Exception ex) {
                event.setRetryCount(event.getRetryCount() + 1);
                outboxEventRepository.save(event);
                log.warn("Failed to publish outbox event id={} to Kafka. Retry count={}. Error: {}",
                        event.getId(), event.getRetryCount(), ex.getMessage());
                // Break loop so subsequent events maintain chronological order
                break;
            }
        }
    }

    private String resolveTopic(String aggregateType) {
        if ("ORDER".equalsIgnoreCase(aggregateType)) {
            return ORDER_EVENTS_TOPIC;
        }
        return "general.events";
    }
}
