package com.swiggy.order;

import com.swiggy.order.client.RestaurantValidationClient;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Stage 5 Integration Tests verifying Production Hardening, Actuator, Prometheus metrics,
 * and Resilience4j Circuit Breaker Fallback handling.
 *
 * <p>SEE: ADR-010 (Resilience, Circuit Breakers & Distributed Observability)
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class ProductionHardeningIntegrationTests {

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
        registry.add("spring.kafka.bootstrap-servers", () -> "127.0.0.1:9092");
        registry.add("spring.kafka.listener.auto-startup", () -> "false");
        registry.add("app.outbox.poll-interval-ms", () -> "99999999");
        registry.add("management.endpoints.web.exposure.include", () -> "*");
        registry.add("management.endpoint.prometheus.enabled", () -> "true");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RestaurantValidationClient restaurantValidationClient;

    @Autowired
    private MeterRegistry meterRegistry;

    @Test
    @DisplayName("Should expose Actuator Health, Metrics, and Resilience4j endpoints")
    void shouldExposeActuatorEndpoints() throws Exception {
        // Health endpoint
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));

        // General Metrics endpoint
        mockMvc.perform(get("/actuator/metrics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.names").isArray());

        // Resilience4j Circuit Breaker Actuator endpoint
        mockMvc.perform(get("/actuator/circuitbreakers"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Should execute Resilience4j fallback and increment metric when target service fails")
    void shouldTriggerResilience4jFallbackOnFailure() {
        UUID testRestaurantId = UUID.randomUUID();

        // 1. Normal execution
        restaurantValidationClient.setSimulateFailure(false);
        boolean initial = restaurantValidationClient.isRestaurantOpen(testRestaurantId);
        assertThat(initial).isTrue();

        // 2. Simulate failure: should not throw exception; fallback method executes gracefully
        restaurantValidationClient.setSimulateFailure(true);
        boolean fallbackResult = restaurantValidationClient.isRestaurantOpen(testRestaurantId);
        assertThat(fallbackResult).isTrue();

        // 3. Verify custom Micrometer metric counter was recorded
        double trips = meterRegistry.get("swiggy.resilience.circuit.trips")
                .tag("target_service", "catalog-service")
                .counter()
                .count();

        assertThat(trips).isGreaterThanOrEqualTo(1.0);
    }
}
