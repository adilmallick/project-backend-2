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
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")

    // DECISION: PostgreSQL JDBC driver for communication with catalog_db.
    runtimeOnly("org.postgresql:postgresql")

    // DECISION: Flyway manages versioned migrations for catalog_db.
    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-database-postgresql")

    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")
    testCompileOnly("org.projectlombok:lombok")
    testAnnotationProcessor("org.projectlombok:lombok")

    // Testing
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.testcontainers:postgresql")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
}
