package com.swiggy.catalog;

import com.swiggy.catalog.dto.CreateRestaurantRequest;
import com.swiggy.catalog.dto.RestaurantResponse;
import com.swiggy.catalog.service.RestaurantService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Stage 2 Chaos Exercise: Simulates catastrophic Redis cluster failure / network partition.
 * Verifies that the catalog service degrades gracefully without failing user requests.
 *
 * <p>SEE: ADR-006 (Redis Caching & Invalidation Strategies)
 */
@SpringBootTest
@Testcontainers
class RedisOutageChaosIntegrationTests {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("catalog_db")
            .withUsername("delivery_user")
            .withPassword("delivery_pass");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        // CHAOS CONFIGURATION: Point Redis to a non-existent port (simulating complete outage)
        registry.add("spring.data.redis.host", () -> "127.0.0.1");
        registry.add("spring.data.redis.port", () -> 65432);
        registry.add("spring.data.redis.timeout", () -> "500ms");
    }

    @Autowired
    private RestaurantService restaurantService;

    @Test
    @DisplayName("Stage 2 Chaos: When Redis is unreachable, queries must transparently fall back to PostgreSQL")
    void shouldGracefullyFallbackToPostgresWhenRedisIsUnreachable() {
        RestaurantResponse created = restaurantService.createRestaurant(new CreateRestaurantRequest(
                "Nagarjuna Andhra Meals",
                "Spicy Andhra Biryani and Meals",
                "Residency Rd, Bangalore",
                UUID.randomUUID()
        ));

        // When Redis is dead, Custom CacheErrorHandler catches the connection failure
        // and allows the method to execute and fetch from PostgreSQL directly.
        RestaurantResponse fetched = restaurantService.getRestaurantById(created.id());

        assertThat(fetched).isNotNull();
        assertThat(fetched.id()).isEqualTo(created.id());
        assertThat(fetched.name()).isEqualTo("Nagarjuna Andhra Meals");
    }
}
