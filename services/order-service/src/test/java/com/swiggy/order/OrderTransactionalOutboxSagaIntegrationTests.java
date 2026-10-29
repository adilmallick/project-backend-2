package com.swiggy.order;

import com.swiggy.order.dto.CreateOrderRequest;
import com.swiggy.order.dto.OrderItemRequest;
import com.swiggy.order.dto.OrderResponse;
import com.swiggy.order.model.OrderStatus;
import com.swiggy.order.outbox.OutboxEvent;
import com.swiggy.order.outbox.OutboxEventRepository;
import com.swiggy.order.outbox.OutboxPublisherService;
import com.swiggy.order.service.OrderService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Stage 3 Integration Tests verifying the Transactional Outbox Pattern,
 * asynchronous Kafka event publishing, and Choreographed Saga Compensation.
 *
 * <p>SEE: ADR-008 (Transactional Outbox Pattern & Event-Driven Sagas)
 */
@SpringBootTest
@Testcontainers
class OrderTransactionalOutboxSagaIntegrationTests {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("order_db")
            .withUsername("delivery_user")
            .withPassword("delivery_pass");

    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
            .withExposedPorts(6379);

    @Container
    static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.6.1"));

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
        registry.add("app.outbox.poll-interval-ms", () -> "500");
    }

    @Autowired
    private OrderService orderService;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Autowired
    private OutboxPublisherService outboxPublisherService;

    @Test
    @DisplayName("Should stage Outbox event, publish to Kafka, and advance order state on successful payment")
    void shouldExecuteSuccessfulOrderSaga() {
        // Standard order under ₹1,000 threshold (₹450)
        CreateOrderRequest request = new CreateOrderRequest(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "12th Main, Indiranagar, Bangalore",
                List.of(new OrderItemRequest(UUID.randomUUID(), "Paneer Tikka Roll", 1, 45000))
        );

        OrderResponse order = orderService.createOrder(request);

        // 1. Verify OutboxEvent staged in PostgreSQL
        List<OutboxEvent> outboxList = outboxEventRepository.findAll();
        assertThat(outboxList).isNotEmpty();
        OutboxEvent staged = outboxList.stream()
                .filter(e -> e.getAggregateId().equals(order.id().toString()))
                .findFirst()
                .orElseThrow();
        assertThat(staged.getEventType()).isEqualTo("ORDER_PLACED");

        // 2. Outbox publisher pushes to Kafka topic 'order.events'
        outboxPublisherService.publishPendingEvents();

        // 3. Await Kafka event choreography:
        // OrderPlacedEvent -> PaymentSimulator (success) -> PaymentCompletedEvent -> OrderSagaCoordinator -> RESTAURANT_ACCEPTED
        await().atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> {
                    OrderResponse current = orderService.getOrderById(order.id());
                    assertThat(current.status()).isEqualTo(OrderStatus.RESTAURANT_ACCEPTED);
                });

        // Verify outbox status updated to PUBLISHED
        OutboxEvent published = outboxEventRepository.findById(staged.getId()).orElseThrow();
        assertThat(published.getStatus()).isEqualTo(OutboxEvent.OutboxStatus.PUBLISHED);
        assertThat(published.getPublishedAt()).isNotNull();
    }

    @Test
    @DisplayName("Should execute compensating transaction (CANCELLED order) when payment declines")
    void shouldExecuteCompensatingTransactionOnPaymentFailure() {
        // High-value order exceeding ₹1,000 failure threshold (₹1,500 = 150,000 cents)
        CreateOrderRequest request = new CreateOrderRequest(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "Whitefield Main Rd, Bangalore",
                List.of(new OrderItemRequest(UUID.randomUUID(), "Family Feast Jumbo Pack", 1, 150000))
        );

        OrderResponse order = orderService.createOrder(request);

        // Trigger outbox publish
        outboxPublisherService.publishPendingEvents();

        // Await compensating transaction:
        // OrderPlacedEvent -> PaymentSimulator (declined) -> PaymentFailedEvent -> OrderSagaCoordinator -> CANCELLED
        await().atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> {
                    OrderResponse current = orderService.getOrderById(order.id());
                    assertThat(current.status()).isEqualTo(OrderStatus.CANCELLED);
                });
    }
}
