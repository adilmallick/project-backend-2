package com.swiggy.dispatch.controller;

import com.swiggy.common.dto.ApiResponse;
import com.swiggy.common.filter.CorrelationIdUtils;
import com.swiggy.dispatch.dto.NearbyRiderResponse;
import com.swiggy.dispatch.dto.UpdateRiderLocationRequest;
import com.swiggy.dispatch.service.RiderDispatchService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for rider geospatial location updates and automated order dispatching.
 *
 * <p>SEE: ADR-007 (Distributed Rate Limiting & Idempotency using Redis)
 */
@RestController
@RequestMapping("/api/v1/dispatch")
@RequiredArgsConstructor
public class RiderDispatchController {

    private final RiderDispatchService riderDispatchService;

    @PostMapping("/riders/location")
    public ResponseEntity<ApiResponse<Void>> updateRiderLocation(
            @Valid @RequestBody UpdateRiderLocationRequest request
    ) {
        riderDispatchService.updateLocation(request.riderId(), request.latitude(), request.longitude());
        return ResponseEntity.ok(ApiResponse.success(null, CorrelationIdUtils.getCorrelationId()));
    }

    @GetMapping("/riders/nearby")
    public ResponseEntity<ApiResponse<List<NearbyRiderResponse>>> getNearbyRiders(
            @RequestParam double latitude,
            @RequestParam double longitude,
            @RequestParam(defaultValue = "5.0") double radiusKm
    ) {
        List<NearbyRiderResponse> response = riderDispatchService.findNearbyRiders(latitude, longitude, radiusKm);
        return ResponseEntity.ok(ApiResponse.success(response, CorrelationIdUtils.getCorrelationId()));
    }

    @PostMapping("/orders/{orderId}/assign-rider")
    public ResponseEntity<ApiResponse<NearbyRiderResponse>> assignNearestRider(
            @PathVariable UUID orderId,
            @RequestParam double restaurantLatitude,
            @RequestParam double restaurantLongitude,
            @RequestParam(defaultValue = "5.0") double radiusKm
    ) {
        NearbyRiderResponse response = riderDispatchService.assignNearestRider(
                orderId, restaurantLatitude, restaurantLongitude, radiusKm
        );
        return ResponseEntity.ok(ApiResponse.success(response, CorrelationIdUtils.getCorrelationId()));
    }
}
