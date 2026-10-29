package com.swiggy.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when an entity (User, Restaurant, Order) cannot be found by its identifier.
 */
public class ResourceNotFoundException extends BusinessException {

    public ResourceNotFoundException(String resourceName, Object identifier) {
        super(
                "RESOURCE_NOT_FOUND",
                String.format("%s with identifier '%s' not found", resourceName, identifier),
                HttpStatus.NOT_FOUND
        );
    }
}
