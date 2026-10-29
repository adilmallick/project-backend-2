package com.swiggy.order;

import com.swiggy.order.dto.CreateOrderRequest;
import com.swiggy.order.dto.OrderItemRequest;
import com.swiggy.order.dto.OrderResponse;
import com.swiggy.order.model.OrderStatus;
import com.swiggy.order.outbox.OutboxEvent;
import com.swiggy.order.outbox.OutboxEventRepository;
import com.swiggy.order.service.OrderService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Stage 3 Chaos Exercise: Simulates total Apache Kafka cluster outage.
 * Verifies that the Transactional Outbox pattern allows order creation to succeed
 * without dropping any customer orders or blocking database writes.
 *
 * <p>SEE: ADR-008 (Transactional Outbox Pattern & Event-Driven Sagas)
 */
@SpringBootTest
@Testcontainers
class OrderOutboxChaosIntegrationTests {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("order_db")
            .withUsername("delivery_user")
            .withPassword("delivery_pass");

    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));

        // CHAOS INJECTION: Point Kafka bootstrap-servers to a non-existent port (Kafka cluster is down)
        registry.add("spring.kafka.bootstrap-servers", () -> "127.0.0.1:65431");
    }

    @Autowired
    private OrderService orderService;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Test
    @DisplayName("Stage 3 Chaos: When Kafka is completely down, orders must still be accepted and staged in Outbox")
    void shouldAcceptOrdersAndStageInOutboxWhenKafkaIsDown() {
        CreateOrderRequest request = new CreateOrderRequest(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "Koramangala 5th Block, Bangalore",
                List.of(new OrderItemRequest(UUID.randomUUID(), "Veg Biryani", 1, 22000))
        );

        // Crucial Architectural Invariant: Order creation does NOT talk directly to Kafka synchronously.
        // It saves Order and stages OutboxEvent within the same PostgreSQL ACID transaction.
        OrderResponse createdOrder = orderService.createOrder(request);

        assertThat(createdOrder).isNotNull();
        assertThat(createdOrder.status()).isEqualTo(OrderStatus.ORDER_PLACED);

        // Verify outbox entry is safely persisted with status PENDING waiting for Kafka broker recovery
        List<OutboxEvent> outboxList = outboxEventRepository.findAll();
        OutboxEvent stagedEvent = outboxList.stream()
                .filter(e -> e.getAggregateId().equals(createdOrder.id().toString()))
                .findFirst()
                .orElseThrow();

        assertThat(stagedEvent.getStatus()).isEqualTo(OutboxEvent.OutboxStatus.PENDING);
        assertThat(stagedEvent.getPublishedAt()).isNull();
    }
}
