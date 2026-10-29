package com.swiggy.user.dto;

import com.swiggy.user.model.User;
import com.swiggy.user.model.UserRole;

import java.time.Instant;
import java.util.UUID;

/**
 * Public User profile response.
 *
 * <p>INVARIANT: Never expose password hashes or internal audit metadata in client-facing DTOs.
 */
public record UserResponse(
        UUID id,
        String email,
        UserRole role,
        String name,
        String phone,
        Instant createdAt
) {
    public static UserResponse fromEntity(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getRole(),
                user.getName(),
                user.getPhone(),
                user.getCreatedAt()
        );
    }
}
