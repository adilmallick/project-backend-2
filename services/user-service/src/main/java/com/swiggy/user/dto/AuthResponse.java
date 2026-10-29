package com.swiggy.user.dto;

import com.swiggy.user.model.UserRole;

import java.util.UUID;

/**
 * Authentication response containing issued JWT access token and user metadata.
 */
public record AuthResponse(
        String accessToken,
        String tokenType,
        long expiresInSeconds,
        UUID userId,
        String email,
        UserRole role
) {
    public static AuthResponse of(String token, long expiresInSeconds, UUID userId, String email, UserRole role) {
        return new AuthResponse(token, "Bearer", expiresInSeconds, userId, email, role);
    }
}
