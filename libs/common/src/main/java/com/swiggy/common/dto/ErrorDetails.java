package com.swiggy.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/**
 * Standardized Error Details payload embedded inside ApiResponse on failures.
 *
 * <p>DECISION: Separate machine-readable error codes (e.g., "RESOURCE_NOT_FOUND")
 * from human-readable error messages for client-side localization and error routing.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorDetails(
        String code,
        String message,
        List<ValidationError> validationErrors
) {
    public static ErrorDetails of(String code, String message) {
        return new ErrorDetails(code, message, null);
    }

    public static ErrorDetails of(String code, String message, List<ValidationError> validationErrors) {
        return new ErrorDetails(code, message, validationErrors);
    }
}
