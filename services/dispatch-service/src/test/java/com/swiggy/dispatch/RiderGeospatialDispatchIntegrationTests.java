package com.swiggy.dispatch;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.swiggy.dispatch.dto.NearbyRiderResponse;
import com.swiggy.dispatch.dto.UpdateRiderLocationRequest;
import com.swiggy.dispatch.service.RiderDispatchService;
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
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Stage 2 Integration Tests verifying Redis GEOADD, GEOSEARCH, and Proximity Rider Dispatching.
 *
 * <p>SEE: ADR-007 (Distributed Rate Limiting & Idempotency using Redis)
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class RiderGeospatialDispatchIntegrationTests {

    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RiderDispatchService riderDispatchService;

    @Test
    @DisplayName("Should index rider GPS locations in Redis GEO and find nearest riders within radius")
    void shouldFindAndAssignNearbyRiders() throws Exception {
        UUID riderClose = UUID.randomUUID();    // 0.3 km from restaurant
        UUID riderMedium = UUID.randomUUID();   // 1.2 km from restaurant
        UUID riderFar = UUID.randomUUID();      // 8.5 km from restaurant (Whitefield)

        // Restaurant coordinates (Koramangala Sony World Signal)
        double restaurantLat = 12.9352;
        double restaurantLon = 77.6245;

        // 1. Post GPS coordinates for riders
        // Rider Close (Koramangala 4th Block)
        mockMvc.perform(post("/api/v1/dispatch/riders/location")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateRiderLocationRequest(
                                riderClose, 12.9340, 77.6220
                        ))))
                .andExpect(status().isOk());

        // Rider Medium (Koramangala 1st Block)
        mockMvc.perform(post("/api/v1/dispatch/riders/location")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateRiderLocationRequest(
                                riderMedium, 12.9280, 77.6320
                        ))))
                .andExpect(status().isOk());

        // Rider Far (Whitefield)
        mockMvc.perform(post("/api/v1/dispatch/riders/location")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateRiderLocationRequest(
                                riderFar, 12.9698, 77.7500
                        ))))
                .andExpect(status().isOk());

        // 2. Query riders within 3.0 km radius: should find riderClose and riderMedium, but NOT riderFar
        mockMvc.perform(get("/api/v1/dispatch/riders/nearby")
                        .param("latitude", String.valueOf(restaurantLat))
                        .param("longitude", String.valueOf(restaurantLon))
                        .param("radiusKm", "3.0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[0].riderId").value(riderClose.toString())); // Closest first

        // 3. Dispatch nearest rider for an order
        UUID orderId = UUID.randomUUID();
        String dispatchResponse = mockMvc.perform(post("/api/v1/dispatch/orders/" + orderId + "/assign-rider")
                        .param("restaurantLatitude", String.valueOf(restaurantLat))
                        .param("restaurantLongitude", String.valueOf(restaurantLon))
                        .param("radiusKm", "3.0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.riderId").value(riderClose.toString()))
                .andReturn()
                .getResponse()
                .getContentAsString();

        // 4. Verify that riderClose is now marked ASSIGNED and no longer returned in available pool
        List<NearbyRiderResponse> availableRiders = riderDispatchService.findNearbyRiders(restaurantLat, restaurantLon, 3.0);
        assertThat(availableRiders).hasSize(1);
        assertThat(availableRiders.get(0).riderId()).isEqualTo(riderMedium);
    }
}
