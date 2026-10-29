package com.swiggy.catalog;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * Entrypoint for Catalog Microservice.
 *
 * <p>SEE: ADR-001 (Microservices Architecture)
 * <p>SEE: ADR-002 (Java 21 Virtual Threads)
 */
@SpringBootApplication
@ComponentScan(basePackages = {"com.swiggy.catalog", "com.swiggy.common"})
public class CatalogServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(CatalogServiceApplication.class, args);
    }
}
