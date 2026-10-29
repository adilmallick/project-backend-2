package com.swiggy.common.exception;

import com.swiggy.common.dto.ApiResponse;
import com.swiggy.common.dto.ErrorDetails;
import com.swiggy.common.dto.ValidationError;
import com.swiggy.common.filter.CorrelationIdUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

/**
 * Centralized Global Exception Handler for consistent microservice API error responses.
 *
 * <p>DECISION: Intercepts all controller exceptions and converts them into standardized {@link ApiResponse}.
 * ALTERNATIVES: Letting exceptions bubble up to Tomcat error pages (rejected: reveals internal stack traces, poor client UX).
 * INVARIANT: Never expose raw exception stack traces or SQL error details to clients in 500 responses.
 * SEE: ADR-001
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException ex) {
        String correlationId = CorrelationIdUtils.getCorrelationId();
        log.warn("Business exception occurred [correlationId={}]: code={}, message={}",
                correlationId, ex.getErrorCode(), ex.getMessage());

        ErrorDetails error = ErrorDetails.of(ex.getErrorCode(), ex.getMessage());
        return ResponseEntity.status(ex.getHttpStatus())
                .body(ApiResponse.failure(error, correlationId));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(MethodArgumentNotValidException ex) {
        String correlationId = CorrelationIdUtils.getCorrelationId();
        log.warn("Input validation failed [correlationId={}]: {} errors",
                correlationId, ex.getBindingResult().getErrorCount());

        List<ValidationError> validationErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> new ValidationError(
                        fieldError.getField(),
                        fieldError.getRejectedValue() != null ? fieldError.getRejectedValue().toString() : "null",
                        fieldError.getDefaultMessage()
                ))
                .toList();

        ErrorDetails error = ErrorDetails.of(
                "VALIDATION_FAILED",
                "Input payload validation failed",
                validationErrors
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.failure(error, correlationId));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpectedException(Exception ex) {
        String correlationId = CorrelationIdUtils.getCorrelationId();
        log.error("Unhandled internal server error [correlationId={}]: ", correlationId, ex);

        ErrorDetails error = ErrorDetails.of(
                "INTERNAL_SERVER_ERROR",
                "An unexpected internal error occurred. Please reference the correlation ID when reporting."
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.failure(error, correlationId));
    }
}
