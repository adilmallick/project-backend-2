package com.swiggy.dispatch;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * Dispatch Service application entry point.
 * Responsible for Rider Geospatial matching (GEOADD, GEOSEARCH) and real-time tracking.
 *
 * <p>SEE: ADR-001 (Multi-Project Microservices Architecture from Day 1)
 * <p>SEE: ADR-007 (Distributed Rate Limiting & Idempotency using Redis)
 */
@SpringBootApplication
@ComponentScan(basePackages = {"com.swiggy.dispatch", "com.swiggy.common"})
public class DispatchServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(DispatchServiceApplication.class, args);
    }
}
