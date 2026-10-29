package com.swiggy.order.controller;

import com.swiggy.common.dto.ApiResponse;
import com.swiggy.common.filter.CorrelationIdUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Health and status probe controller for order-service.
 */
@RestController
@RequestMapping("/api/v1/health")
public class HealthController {

    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, String>>> health() {
        Map<String, String> healthData = Map.of(
                "service", "order-service",
                "status", "UP"
        );
        return ResponseEntity.ok(ApiResponse.success(healthData, CorrelationIdUtils.getCorrelationId()));
    }
}
