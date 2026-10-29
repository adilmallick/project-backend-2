package com.swiggy.common.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Common OpenAPI 3.0 configuration providing unified documentation metadata
 * and JWT Bearer security scheme in Swagger UI across all microservices.
 *
 * <p>DECISION: Define OpenApiConfig centrally in libs:common so every microservice
 * inherits consistent security headers, metadata, and interactive Swagger UI.
 * <p>ALTERNATIVES: Per-service OpenApiConfig leads to duplicate security scheme definitions.
 */
@Configuration
@ConditionalOnWebApplication
public class OpenApiConfig {

    public static final String BEARER_AUTH_SCHEME = "BearerAuth";

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Swiggy Mini Food Delivery Platform API")
                        .version("v1.0.0")
                        .description("Production-grade microservices platform powered by Spring Boot 3.4, Java 21, PostgreSQL, Redis, and Apache Kafka.")
                        .contact(new Contact().name("Platform Engineering Team").email("engineering@swiggy-mini.local"))
                        .license(new License().name("Apache 2.0").url("https://spring.io")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_AUTH_SCHEME))
                .components(new Components()
                        .addSecuritySchemes(BEARER_AUTH_SCHEME, new SecurityScheme()
                                .name(BEARER_AUTH_SCHEME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Enter stateless JWT access token received from /api/v1/auth/login or /api/v1/auth/register")));
    }
}
