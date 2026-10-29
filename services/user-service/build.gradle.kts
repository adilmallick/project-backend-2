plugins {
    id("org.springframework.boot")
    id("io.spring.dependency-management")
}

dependencies {
    // DECISION: Depend on internal shared library for common DTOs, exception handlers, and correlation filters.
    // SEE: ADR-001
    implementation(project(":libs:common"))

    // DECISION: Standard Spring MVC web starter.
    implementation("org.springframework.boot:spring-boot-starter-web")

    // DECISION: Validation constraints for user registration and login payloads.
    implementation("org.springframework.boot:spring-boot-starter-validation")

    // DECISION: Spring Data JPA for User entity persistence and repository queries.
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")

    // DECISION: PostgreSQL JDBC driver for communication with user_db.
    runtimeOnly("org.postgresql:postgresql")

    // DECISION: Flyway manages versioned migrations for user_db.
    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-database-postgresql")

    // DECISION: Lombok for boilerplate reduction in JPA entities and DTOs.
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")
    testCompileOnly("org.projectlombok:lombok")
    testAnnotationProcessor("org.projectlombok:lombok")

    // DECISION: Testcontainers provisions real PostgreSQL for integration tests.
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.testcontainers:postgresql")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
}
