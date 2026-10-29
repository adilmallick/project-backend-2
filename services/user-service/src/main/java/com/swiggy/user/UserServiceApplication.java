package com.swiggy.user;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * Entrypoint for User Microservice.
 *
 * <p>DECISION: ComponentScan includes both user service packages and common library components
 * (such as {@link com.swiggy.common.filter.CorrelationIdFilter} and {@link com.swiggy.common.exception.GlobalExceptionHandler}).
 *
 * <p>SEE: ADR-001 (Microservices Architecture)
 * <p>SEE: ADR-002 (Java 21 Virtual Threads)
 */
@SpringBootApplication
@ComponentScan(basePackages = {"com.swiggy.user", "com.swiggy.common"})
public class UserServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(UserServiceApplication.class, args);
    }
}
