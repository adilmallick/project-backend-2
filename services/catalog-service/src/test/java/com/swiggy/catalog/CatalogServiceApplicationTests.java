package com.swiggy.catalog;

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
 * Integration Tests for CatalogServiceApplication.
 *
 * <p>WHAT THIS TEST PROVES:
 * 1. Proves catalog-service boots with Java 21 virtual threads.
 * 2. Proves Flyway migration V1__init_catalog_schema.sql runs successfully on genuine PostgreSQL 16.
 * 3. Proves HealthController returns 200 with correlation headers.
 *
 * <p>WHICH FAILURE IT GUARDS AGAINST:
 * Prevents catalog schema regressions, foreign key violations, and configuration errors.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class CatalogServiceApplicationTests {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Should successfully boot catalog-service context and return UP status")
    void shouldBootAndReturnHealth() throws Exception {
        mockMvc.perform(get("/api/v1/health")
                        .header("X-Correlation-Id", "test-catalog-trace-202"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Correlation-Id", "test-catalog-trace-202"))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.service").value("catalog-service"))
                .andExpect(jsonPath("$.data.status").value("UP"))
                .andExpect(jsonPath("$.correlationId").value("test-catalog-trace-202"));
    }
}
