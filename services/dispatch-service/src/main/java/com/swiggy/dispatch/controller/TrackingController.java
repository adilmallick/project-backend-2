package com.swiggy.dispatch.controller;

import com.swiggy.common.dto.ApiResponse;
import com.swiggy.common.filter.CorrelationIdUtils;
import com.swiggy.dispatch.dto.TrackingLocationPayload;
import com.swiggy.dispatch.tracking.TrackingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Controller exposing REST and STOMP WebSocket interfaces for real-time delivery telemetry.
 *
 * <p>SEE: ADR-009 (Real-Time Location Streaming with STOMP & Redis Backplane)
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class TrackingController {

    private final TrackingService trackingService;

    /**
     * REST endpoint allowing rider apps to POST GPS breadcrumbs over HTTP/2.
     */
    @PostMapping("/api/v1/dispatch/orders/{orderId}/tracking")
    public ResponseEntity<ApiResponse<Void>> postTrackingBreadcrumb(
            @PathVariable UUID orderId,
            @RequestBody TrackingLocationPayload request
    ) {
        TrackingLocationPayload payload = new TrackingLocationPayload(
                orderId,
                request.riderId(),
                request.latitude(),
                request.longitude(),
                request.status() != null ? request.status() : "IN_TRANSIT",
                Instant.now()
        );
        trackingService.publishTrackingUpdate(payload);
        return ResponseEntity.ok(ApiResponse.success(null, CorrelationIdUtils.getCorrelationId()));
    }

    /**
     * STOMP WebSocket endpoint for persistent bidirectional streaming from rider devices.
     */
    @MessageMapping("/rider/location")
    public void handleRiderLocationFrame(@Payload TrackingLocationPayload payload) {
        log.debug("Received STOMP telemetry frame: orderId={}, riderId={}", payload.orderId(), payload.riderId());
        trackingService.publishTrackingUpdate(payload);
    }
}
