// DECISION: Multi-project Gradle Monorepo.
// Grouping modules into 'libs' (shared libraries) and 'services' (deployable Spring Boot applications).
// SEE: ADR-001

rootProject.name = "delivery-platform"

// Shared library modules
include("libs:common")

// Independent Spring Boot microservices
include("services:user-service")
include("services:catalog-service")
include("services:order-service")
