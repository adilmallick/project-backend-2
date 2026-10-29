package com.swiggy.order;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.swiggy.order.dto.CreateOrderRequest;
import com.swiggy.order.dto.OrderItemRequest;
import com.swiggy.order.dto.UpdateOrderStatusRequest;
import com.swiggy.order.model.Order;
import com.swiggy.order.model.OrderStatus;
import com.swiggy.order.repository.OrderRepository;
import com.swiggy.order.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-End Integration and Concurrency Chaos Tests for Order Lifecycle.
 *
 * <p>WHAT THIS TEST PROVES:
 * 1. Proves orders and line items persist to order_db with exact integer cent totals.
 * 2. Proves the Order State Machine transitions forward along valid paths.
 * 3. Proves illegal/backward state transitions are rejected with 409 Conflict.
 * 4. CHAOS EXERCISE: Proves JPA @Version optimistic locking detects concurrent update collisions
 *    and prevents the Lost Update anomaly without holding blocking database locks.
 *
 * <p>SEE: ADR-005
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class OrderLifecycleIntegrationTests {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderService orderService;

    @BeforeEach
    void setUp() {
        orderRepository.deleteAll();
    }

    private CreateOrderRequest buildSampleOrderRequest() {
        UUID customerId = UUID.randomUUID();
        UUID restaurantId = UUID.randomUUID();

        List<OrderItemRequest> items = List.of(
                new OrderItemRequest(UUID.randomUUID(), "Paneer Butter Masala", 2, 850), // 2 * 850 = 1700
                new OrderItemRequest(UUID.randomUUID(), "Butter Naan", 4, 150)           // 4 * 150 = 600 -> Total: 2300 cents
        );

        return new CreateOrderRequest(customerId, restaurantId, "Flat 402, Green Heights, Bangalore", items);
    }

    @Test
    @DisplayName("Should create an order, calculate total in integer cents, and initialize version to 0")
    void shouldCreateOrderAndCalculateTotalCents() throws Exception {
        CreateOrderRequest request = buildSampleOrderRequest();

        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("ORDER_PLACED"))
                .andExpect(jsonPath("$.data.totalAmountCents").value(2300))
                .andExpect(jsonPath("$.data.version").value(0))
                .andExpect(jsonPath("$.data.items", hasSize(2)));
    }

    @Test
    @DisplayName("Should transition order forward through valid state machine path")
    void shouldTransitionOrderForwardThroughLifecycle() throws Exception {
        CreateOrderRequest request = buildSampleOrderRequest();
        var orderResponse = orderService.createOrder(request);
        UUID orderId = orderResponse.id();

        // 1. ORDER_PLACED -> RESTAURANT_ACCEPTED
        mockMvc.perform(patch("/api/v1/orders/" + orderId + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateOrderStatusRequest(OrderStatus.RESTAURANT_ACCEPTED))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("RESTAURANT_ACCEPTED"))
                .andExpect(jsonPath("$.data.version").value(1));

        // 2. RESTAURANT_ACCEPTED -> RIDER_ASSIGNED
        mockMvc.perform(patch("/api/v1/orders/" + orderId + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateOrderStatusRequest(OrderStatus.RIDER_ASSIGNED))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("RIDER_ASSIGNED"))
                .andExpect(jsonPath("$.data.version").value(2));

        // 3. RIDER_ASSIGNED -> PICKED_UP
        mockMvc.perform(patch("/api/v1/orders/" + orderId + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateOrderStatusRequest(OrderStatus.PICKED_UP))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PICKED_UP"))
                .andExpect(jsonPath("$.data.version").value(3));

        // 4. PICKED_UP -> DELIVERED
        mockMvc.perform(patch("/api/v1/orders/" + orderId + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateOrderStatusRequest(OrderStatus.DELIVERED))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("DELIVERED"))
                .andExpect(jsonPath("$.data.version").value(4));
    }

    @Test
    @DisplayName("Should reject backward or illegal state transition with 409 Conflict")
    void shouldRejectInvalidStateTransition() throws Exception {
        CreateOrderRequest request = buildSampleOrderRequest();
        var orderResponse = orderService.createOrder(request);
        UUID orderId = orderResponse.id();

        // Attempting to jump directly from ORDER_PLACED to DELIVERED is illegal
        mockMvc.perform(patch("/api/v1/orders/" + orderId + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateOrderStatusRequest(OrderStatus.DELIVERED))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("INVALID_STATUS_TRANSITION"));
    }

    // =========================================================================
    // STAGE 1 CHAOS EXERCISE: Concurrent Order Modifications
    // =========================================================================
    @Test
    @DisplayName("CHAOS EXERCISE: Concurrent updates to the same order must trigger OptimisticLockingFailureException")
    void chaosExercise_concurrentOrderUpdates_shouldTriggerOptimisticLockException() {
        CreateOrderRequest request = buildSampleOrderRequest();
        var initialOrder = orderService.createOrder(request);
        UUID orderId = initialOrder.id();

        // Thread A loads Order at version 0
        Order orderInstanceA = orderRepository.findById(orderId).orElseThrow();
        assertThat(orderInstanceA.getVersion()).isEqualTo(0L);

        // Thread B loads the EXACT same Order at version 0
        Order orderInstanceB = orderRepository.findById(orderId).orElseThrow();
        assertThat(orderInstanceB.getVersion()).isEqualTo(0L);

        // Step 1: Thread A accepts the order and commits -> increments DB version to 1
        orderInstanceA.transitionTo(OrderStatus.RESTAURANT_ACCEPTED);
        orderRepository.saveAndFlush(orderInstanceA);

        Order dbAfterThreadA = orderRepository.findById(orderId).orElseThrow();
        assertThat(dbAfterThreadA.getVersion()).isEqualTo(1L);
        assertThat(dbAfterThreadA.getStatus()).isEqualTo(OrderStatus.RESTAURANT_ACCEPTED);

        // Step 2: Thread B attempts to cancel the order holding the STALE version 0
        orderInstanceB.transitionTo(OrderStatus.CANCELLED);

        // INVARIANT: Hibernate must detect that the version in DB (1) does not match orderInstanceB (0).
        // It must throw OptimisticLockingFailureException and ABORT the save, preventing Lost Update!
        assertThatThrownBy(() -> orderRepository.saveAndFlush(orderInstanceB))
                .isInstanceOf(OptimisticLockingFailureException.class);

        // Verify database state: Thread A's update is intact, Thread B was prevented from corrupting state
        Order finalOrder = orderRepository.findById(orderId).orElseThrow();
        assertThat(finalOrder.getStatus()).isEqualTo(OrderStatus.RESTAURANT_ACCEPTED);
        assertThat(finalOrder.getVersion()).isEqualTo(1L);
    }
}
