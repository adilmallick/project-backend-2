# =============================================================================
# Production Dockerfile: order-service (Orders, State Machine & Sagas)
# DECISION: Minimal Alpine Temurin JRE base image with non-root security context.
# ALTERNATIVES: Full JDK or Debian-based images (larger attack surface and image size).
# TRADE-OFF: Alpine musl libc is tiny (~180MB) but requires standard POSIX compatibility.
# INVARIANT: Containers must run as unprivileged user 'appuser' (UID 10001).
# SEE: ADR-001, ADR-005, ADR-008, ADR-010, ADR-011
# =============================================================================

FROM eclipse-temurin:21-jre-alpine

RUN apk add --no-cache curl tzdata \
    && addgroup -S appgroup -g 10001 \
    && adduser -S appuser -u 10001 -G appgroup

WORKDIR /app

ARG JAR_FILE=build/libs/order-service-0.0.1-SNAPSHOT.jar
COPY ${JAR_FILE} app.jar

RUN chown -R appuser:appgroup /app

USER appuser

EXPOSE 8083

HEALTHCHECK --interval=15s --timeout=5s --start-period=30s --retries=3 \
  CMD curl -f http://localhost:8083/actuator/health || exit 1

ENTRYPOINT ["java", \
  "-XX:+UseG1GC", \
  "-XX:MaxRAMPercentage=75.0", \
  "-XX:+ExitOnOutOfMemoryError", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.jar"]
