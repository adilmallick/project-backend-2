package com.swiggy.order;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.swiggy.order.dto.CreateOrderRequest;
import com.swiggy.order.dto.OrderItemRequest;
import com.swiggy.order.repository.OrderRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Stage 2 Integration Tests verifying Redis Distributed Idempotency on Order Creation.
 *
 * <p>SEE: ADR-007 (Distributed Rate Limiting & Idempotency using Redis)
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class OrderIdempotencyIntegrationTests {

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
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OrderRepository orderRepository;

    @Test
    @DisplayName("Should create order on first request and return cached response on duplicate idempotent request")
    void shouldHandleIdempotentOrderCreation() throws Exception {
        UUID customerId = UUID.randomUUID();
        UUID restaurantId = UUID.randomUUID();
        String idempotencyKey = UUID.randomUUID().toString();

        CreateOrderRequest request = new CreateOrderRequest(
                customerId,
                restaurantId,
                "Tower B, Flat 802, Prestige Shantiniketan, Bangalore",
                List.of(
                        new OrderItemRequest(UUID.randomUUID(), "Chicken Dum Biryani", 2, 35000),
                        new OrderItemRequest(UUID.randomUUID(), "Gulab Jamun", 1, 8000)
                )
        );

        String jsonPayload = objectMapper.writeValueAsString(request);

        // 1. First request with Idempotency-Key -> MISS, created fresh in DB
        String firstResponse = mockMvc.perform(post("/api/v1/orders")
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isCreated())
                .andExpect(header().string("X-Cache-Lookup", "MISS"))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalAmountCents").value(78000))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String createdOrderId = objectMapper.readTree(firstResponse).get("data").get("id").asText();

        // Verify exactly 1 order in Postgres database
        long initialCount = orderRepository.count();

        // 2. Client retry / network timeout duplicate request with identical Idempotency-Key
        mockMvc.perform(post("/api/v1/orders")
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Cache-Lookup", "HIT"))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(createdOrderId))
                .andExpect(jsonPath("$.data.totalAmountCents").value(78000));

        // Invariant: No duplicate database record was inserted!
        assertThat(orderRepository.count()).isEqualTo(initialCount);
    }
}
