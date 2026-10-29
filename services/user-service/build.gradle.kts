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

    // DECISION: Validation constraints for user registration and login payloads (@NotBlank, @Email, @Size).
    implementation("org.springframework.boot:spring-boot-starter-validation")

    // =========================================================================
    // Security & Authentication
    // =========================================================================
    // DECISION: Spring Security provides battle-tested filter chain infrastructure,
    // SecurityContext management, and BCryptPasswordEncoder.
    // ALTERNATIVES: Custom servlet authentication filters (rejected: high risk of security misconfigurations).
    // SEE: ADR-003
    implementation("org.springframework.boot:spring-boot-starter-security")

    // DECISION: JJWT (Java JWT) library for creating, signing, and parsing stateless JSON Web Tokens.
    // ALTERNATIVES: spring-security-oauth2-resource-server (rejected for learning: auto-config hides token parsing internals).
    // ALTERNATIVES: nimbus-jose-jwt (viable, but JJWT provides more ergonomic fluent builder APIs).
    // SEE: ADR-003
    implementation("io.jsonwebtoken:jjwt-api:0.12.6")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.6")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.6")

    // =========================================================================
    // Persistence & Relational Database (user_db)
    // =========================================================================
    // DECISION: Spring Data JPA for User entity persistence and repository queries.
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")

    // DECISION: PostgreSQL JDBC driver for communication with user_db.
    runtimeOnly("org.postgresql:postgresql")

    // DECISION: Flyway manages versioned migrations for user_db.
    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-database-postgresql")

    // DECISION: Redis for sliding-window rate limiting on authentication and sensitive endpoints.
    // SEE: ADR-007
    implementation("org.springframework.boot:spring-boot-starter-data-redis")

    // DECISION: Lombok for boilerplate reduction in JPA entities and DTOs.
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")
    testCompileOnly("org.projectlombok:lombok")
    testAnnotationProcessor("org.projectlombok:lombok")

    // =========================================================================
    // Testing & Verification
    // =========================================================================
    // DECISION: Testcontainers provisions real PostgreSQL for integration tests.
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.testcontainers:postgresql")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
}
