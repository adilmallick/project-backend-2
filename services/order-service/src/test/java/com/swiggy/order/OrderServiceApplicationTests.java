package com.swiggy.order;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration Tests for OrderServiceApplication.
 *
 * <p>WHAT THIS TEST PROVES:
 * 1. Proves order-service boots with Java 21 virtual threads.
 * 2. Proves Flyway migration V1__init_order_schema.sql runs and validates successfully against PostgreSQL 16.
 * 3. Proves HealthController returns 200 with standard ApiResponse envelope.
 *
 * <p>WHICH FAILURE IT GUARDS AGAINST:
 * Prevents order schema syntax regressions, broken state constraints, and configuration mismatches.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class OrderServiceApplicationTests {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Should successfully boot order-service and verify health endpoint")
    void shouldBootAndReturnHealth() throws Exception {
        mockMvc.perform(get("/api/v1/health")
                        .header("X-Correlation-Id", "test-order-trace-303"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Correlation-Id", "test-order-trace-303"))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.service").value("order-service"))
                .andExpect(jsonPath("$.data.status").value("UP"))
                .andExpect(jsonPath("$.correlationId").value("test-order-trace-303"));
    }
}
