package com.swiggy.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Base unchecked business exception for domain and application errors.
 *
 * <p>DECISION: Inherit from RuntimeException to avoid forcing callers to declare checked exceptions,
 * while encapsulating HTTP status and machine-readable error codes.
 */
public class BusinessException extends RuntimeException {

    private final String errorCode;
    private final HttpStatus httpStatus;

    public BusinessException(String errorCode, String message, HttpStatus httpStatus) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }

    public BusinessException(String errorCode, String message) {
        this(errorCode, message, HttpStatus.BAD_REQUEST);
    }

    public String getErrorCode() {
        return errorCode;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }
}
