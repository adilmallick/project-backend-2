plugins {
    `java-library`
}

dependencies {
    // DECISION: Spring Web starter provided as API so consuming services get web annotations and Jackson serialization.
    api("org.springframework.boot:spring-boot-starter-web")

    // DECISION: Jakarta Bean validation annotations for DTO constraints.
    api("org.springframework.boot:spring-boot-starter-validation")

    // DECISION: OpenAPI 3 / Swagger UI starter for interactive API exploration across all microservices.
    api("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.5")

    // =========================================================================
    // Stage 5: Production Hardening, Observability & Resilience
    // =========================================================================
    // DECISION: Spring Boot Actuator exposes health, info, and metric endpoints.
    api("org.springframework.boot:spring-boot-starter-actuator")

    // DECISION: Micrometer Prometheus registry for Prometheus scraping.
    api("io.micrometer:micrometer-registry-prometheus")

    // DECISION: OpenTelemetry Micrometer Tracing bridge for distributed trace/span context propagation.
    api("io.micrometer:micrometer-tracing-bridge-otel")

    // DECISION: Resilience4j circuit breakers, retries, and time limiters.
    api("io.github.resilience4j:resilience4j-spring-boot3:2.2.0")
    api("org.springframework.boot:spring-boot-starter-aop")

    // DECISION: Lombok for boilerplate reduction in common DTOs.
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")
    testCompileOnly("org.projectlombok:lombok")
    testAnnotationProcessor("org.projectlombok:lombok")

    // DECISION: JUnit 5 and AssertJ for testing common utilities and filters.
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}
