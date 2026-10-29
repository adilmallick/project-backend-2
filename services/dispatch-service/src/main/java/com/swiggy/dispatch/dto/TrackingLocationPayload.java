package com.swiggy.dispatch.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Real-time location and status payload streamed over STOMP WebSocket and Redis Pub/Sub.
 *
 * <p>SEE: ADR-009 (Real-Time Location Streaming with STOMP & Redis Backplane)
 */
public record TrackingLocationPayload(
        UUID orderId,
        UUID riderId,
        double latitude,
        double longitude,
        String status,
        Instant timestamp
) {}
