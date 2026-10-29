package com.swiggy.order;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * Entrypoint for Order Microservice.
 *
 * <p>SEE: ADR-001 (Microservices Architecture)
 * <p>SEE: ADR-002 (Java 21 Virtual Threads)
 */
@SpringBootApplication
@ComponentScan(basePackages = {"com.swiggy.order", "com.swiggy.common"})
public class OrderServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrderServiceApplication.class, args);
    }
}
