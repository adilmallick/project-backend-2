package com.swiggy.common.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * Common Micrometer Metrics Service providing platform-wide telemetry.
 *
 * <p>DECISION: Standardize metric names, tags, and distribution timers across all microservices
 * so Prometheus dashboards and alerts ingest uniform metric labels.
 *
 * <p>SEE: ADR-010 (Resilience, Circuit Breakers & Distributed Observability)
 */
@Service
public class PlatformMetricsService {

    private final MeterRegistry meterRegistry;

    public PlatformMetricsService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public void incrementOrdersCreated(String status) {
        Counter.builder("swiggy.orders.created.total")
                .description("Total number of food delivery orders created")
                .tag("status", status)
                .register(meterRegistry)
                .increment();
    }

    public void recordOrderProcessingTime(long durationMillis) {
        Timer.builder("swiggy.order.processing.duration")
                .description("Time taken to process and stage orders")
                .publishPercentileHistogram()
                .register(meterRegistry)
                .record(durationMillis, TimeUnit.MILLISECONDS);
    }

    public void incrementCircuitBreakerTrips(String serviceName) {
        Counter.builder("swiggy.resilience.circuit.trips")
                .description("Number of circuit breaker trip events")
                .tag("target_service", serviceName)
                .register(meterRegistry)
                .increment();
    }
}
