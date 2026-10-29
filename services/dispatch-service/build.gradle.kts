plugins {
    id("org.springframework.boot")
    id("io.spring.dependency-management")
}

dependencies {
    // DECISION: Depend on internal shared library for common DTOs, exception handlers, and correlation filters.
    // SEE: ADR-001
    implementation(project(":libs:common"))

    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-validation")

    // DECISION: Redis starter for Geospatial queries (GEOADD, GEOSEARCH) and real-time Pub/Sub message broker.
    // SEE: ADR-006, ADR-007
    implementation("org.springframework.boot:spring-boot-starter-data-redis")

    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")
    testCompileOnly("org.projectlombok:lombok")
    testAnnotationProcessor("org.projectlombok:lombok")

    // Testing
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
}
