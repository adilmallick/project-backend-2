package com.swiggy.catalog;

import com.swiggy.catalog.dto.CreateMenuItemRequest;
import com.swiggy.catalog.dto.CreateRestaurantRequest;
import com.swiggy.catalog.dto.RestaurantResponse;
import com.swiggy.catalog.repository.RestaurantRepository;
import com.swiggy.catalog.service.RestaurantService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Stage 2 Integration Tests verifying Redis Menu Caching and Cache Eviction.
 *
 * <p>SEE: ADR-006 (Redis Caching & Invalidation Strategies)
 */
@SpringBootTest
@Testcontainers
class RestaurantCacheIntegrationTests {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("catalog_db")
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
    private RestaurantService restaurantService;

    @Autowired
    private RestaurantRepository restaurantRepository;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Test
    @DisplayName("Should cache restaurant details in Redis on first call and evict on menu update")
    void shouldCacheAndEvictRestaurant() {
        // 1. Create a restaurant
        CreateRestaurantRequest createReq = new CreateRestaurantRequest(
                "Truffles Koramangala",
                "Famous burgers and steaks",
                "80 Feet Rd, Koramangala, Bangalore",
                UUID.randomUUID()
        );
        RestaurantResponse created = restaurantService.createRestaurant(createReq);
        UUID restaurantId = created.id();

        // Initially, redis should not have this key
        String redisKey = "restaurants::" + restaurantId;
        Boolean existsBefore = redisTemplate.hasKey(redisKey);
        assertThat(existsBefore).isFalse();

        // 2. First call -> Cache MISS, populates Redis
        RestaurantResponse firstFetch = restaurantService.getRestaurantById(restaurantId);
        assertThat(firstFetch.name()).isEqualTo("Truffles Koramangala");

        Boolean existsAfter = redisTemplate.hasKey(redisKey);
        assertThat(existsAfter).isTrue();

        // 3. Second call -> Cache HIT from Redis
        RestaurantResponse secondFetch = restaurantService.getRestaurantById(restaurantId);
        assertThat(secondFetch.name()).isEqualTo("Truffles Koramangala");

        // 4. Update menu item -> Evicts Redis cache
        restaurantService.addMenuItem(restaurantId, new CreateMenuItemRequest(
                "All American Cheese Burger",
                "Juicy tenderloin patty with cheddar",
                35000
        ));

        Boolean existsAfterEvict = redisTemplate.hasKey(redisKey);
        assertThat(existsAfterEvict).isFalse();

        // 5. Next fetch -> Reloads fresh data from DB with updated menu item
        RestaurantResponse refreshedFetch = restaurantService.getRestaurantById(restaurantId);
        assertThat(refreshedFetch.menuItems()).hasSize(1);
        assertThat(refreshedFetch.menuItems().get(0).name()).isEqualTo("All American Cheese Burger");

        // Key re-cached
        assertThat(redisTemplate.hasKey(redisKey)).isTrue();
    }
}
