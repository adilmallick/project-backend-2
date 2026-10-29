package com.swiggy.dispatch.dto;

import java.util.UUID;

public record NearbyRiderResponse(
        UUID riderId,
        double distanceKm,
        double latitude,
        double longitude
) {}
