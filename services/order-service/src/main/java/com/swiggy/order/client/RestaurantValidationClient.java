package com.swiggy.order.client;

import com.swiggy.common.metrics.PlatformMetricsService;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Client demonstrating Resilience4j Circuit Breakers, Retries, and Fallback handlers.
 *
 * <p>Prevents cascading failures when downstream microservices experience latency or outages.
 *
 * <p>SEE: ADR-010 (Resilience, Circuit Breakers & Distributed Observability)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RestaurantValidationClient {

    private final PlatformMetricsService platformMetricsService;

    // Simulation flag for testing circuit breaker trips
    private volatile boolean simulateFailure = false;

    public void setSimulateFailure(boolean simulate) {
        this.simulateFailure = simulate;
    }

    @CircuitBreaker(name = "catalogService", fallbackMethod = "validateRestaurantFallback")
    @Retry(name = "catalogService")
    public boolean isRestaurantOpen(UUID restaurantId) {
        if (simulateFailure) {
            log.error("Simulated network timeout/connection failure when contacting catalog-service for restaurantId={}",
                    restaurantId);
            throw new RuntimeException("503 Service Unavailable: Catalog service connection timed out");
        }

        log.debug("Remote check: restaurantId={} is OPEN and accepting orders", restaurantId);
        return true;
    }

    /**
     * Fallback method executed when catalog-service fails or circuit breaker opens.
     */
    public boolean validateRestaurantFallback(UUID restaurantId, Throwable throwable) {
        log.warn("Circuit Breaker FALLBACK invoked for restaurantId={}: catalog-service is DOWN. Defaulting to OPEN. Error: {}",
                restaurantId, throwable.getMessage());
        platformMetricsService.incrementCircuitBreakerTrips("catalog-service");
        return true; // Graceful degradation: allow order placement to proceed
    }
}
