package com.swiggy.user.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Strongly-typed configuration properties for JWT issuing.
 *
 * <p>SEE: ADR-003
 */
@ConfigurationProperties(prefix = "security.jwt")
public record JwtProperties(
        String secret,
        long expirationMinutes
) {}
