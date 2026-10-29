package com.swiggy.user.controller;

import com.swiggy.common.dto.ApiResponse;
import com.swiggy.common.filter.CorrelationIdUtils;
import com.swiggy.user.dto.AuthResponse;
import com.swiggy.user.dto.LoginRequest;
import com.swiggy.user.dto.RegisterRequest;
import com.swiggy.user.ratelimit.RateLimit;
import com.swiggy.user.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller exposing public authentication endpoints.
 *
 * <p>SEE: ADR-003 (Stateless JWT Authentication with RBAC)
 * <p>SEE: ADR-007 (Distributed Rate Limiting & Idempotency using Redis)
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @RateLimit(maxRequests = 10, windowSeconds = 60)
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, CorrelationIdUtils.getCorrelationId()));
    }

    @PostMapping("/login")
    @RateLimit(maxRequests = 5, windowSeconds = 10)
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success(response, CorrelationIdUtils.getCorrelationId()));
    }
}
