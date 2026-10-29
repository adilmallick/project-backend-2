package com.swiggy.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;

/**
 * Standard API Envelope for all HTTP responses across microservices.
 *
 * <p>DECISION: Standardize all API responses (success and error) into a single predictable envelope.
 * ALTERNATIVES: Returning raw entity DTOs directly (rejected: frontend clients must handle inconsistent payload shapes).
 * TRADE-OFF: Adds a minor JSON nesting level ({@code data: {...}}).
 * INVARIANT: Every response must contain a non-null timestamp and correlationId for distributed observability.
 * SEE: ADR-001
 *
 * @param <T> Response payload type
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(
        boolean success,
        T data,
        ErrorDetails error,
        Instant timestamp,
        String correlationId
) {
    public static <T> ApiResponse<T> success(T data, String correlationId) {
        return new ApiResponse<>(true, data, null, Instant.now(), correlationId);
    }

    public static <T> ApiResponse<T> failure(ErrorDetails error, String correlationId) {
        return new ApiResponse<>(false, null, error, Instant.now(), correlationId);
    }
}
