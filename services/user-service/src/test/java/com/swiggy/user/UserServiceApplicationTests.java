package com.swiggy.user;

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
 * Integration Tests for UserServiceApplication.
 *
 * <p>DECISION: Uses Testcontainers with PostgreSQL 16 image instead of H2.
 *
 * <p>WHAT THIS TEST PROVES:
 * 1. Proves Spring context boots with Java 21 virtual threads enabled.
 * 2. Proves Flyway migration runs and executes V1__init_user_schema.sql cleanly against genuine PostgreSQL.
 * 3. Proves HealthController returns 200 with standard ApiResponse envelope and correlation header.
 *
 * <p>WHICH FAILURE IT GUARDS AGAINST:
 * Guards against broken SQL syntax in migrations, database dialect mismatches, and configuration misalignments.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class UserServiceApplicationTests {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Should successfully boot application context and verify health endpoint with correlation ID")
    void shouldBootAndReturnHealth() throws Exception {
        mockMvc.perform(get("/api/v1/health")
                        .header("X-Correlation-Id", "test-user-trace-101"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Correlation-Id", "test-user-trace-101"))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.service").value("user-service"))
                .andExpect(jsonPath("$.data.status").value("UP"))
                .andExpect(jsonPath("$.correlationId").value("test-user-trace-101"));
    }
}
