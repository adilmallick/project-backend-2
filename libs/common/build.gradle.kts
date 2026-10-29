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

    // DECISION: Lombok for boilerplate reduction in common DTOs.
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")
    testCompileOnly("org.projectlombok:lombok")
    testAnnotationProcessor("org.projectlombok:lombok")

    // DECISION: JUnit 5 and AssertJ for testing common utilities and filters.
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}
