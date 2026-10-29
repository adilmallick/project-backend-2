package com.swiggy.common.dto;

/**
 * Details of a single input validation violation.
 */
public record ValidationError(
        String field,
        String rejectedValue,
        String message
) {}
