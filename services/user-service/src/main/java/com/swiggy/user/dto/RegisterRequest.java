package com.swiggy.user.dto;

import com.swiggy.user.model.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Registration request payload with declarative Bean Validation constraints.
 *
 * <p>DECISION: Uses Java records for immutable data carrier semantics.
 *
 * <p>INVARIANT: Password must be at least 8 characters to meet baseline entropy requirements.
 */
public record RegisterRequest(
        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        String email,

        @NotBlank(message = "Password is required")
        @Size(min = 8, max = 100, message = "Password must be between 8 and 100 characters")
        String password,

        @NotNull(message = "User role is required (CUSTOMER, RESTAURANT_OWNER, RIDER)")
        UserRole role,

        @NotBlank(message = "Name is required")
        @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
        String name,

        @NotBlank(message = "Phone number is required")
        @Pattern(regexp = "^\\+?[1-9]\\d{9,14}$", message = "Phone must be a valid E.164 phone number (10-15 digits)")
        String phone
) {}
