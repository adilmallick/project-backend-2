package com.swiggy.dispatch.service;

import com.swiggy.common.exception.BusinessException;
import com.swiggy.dispatch.dto.NearbyRiderResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.geo.Circle;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.geo.Metrics;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Service managing real-time rider locations and proximity dispatching using Redis Geospatial indices.
 *
 * <p>DECISION: Uses Redis GEOADD and GEOSEARCH (radius) commands. Under the hood, Redis encodes
 * (longitude, latitude) into a 52-bit integer using Geohash, stored in a standard Sorted Set (ZSET).
 * Distance calculations execute at lightning speed (< 1ms) in memory without costly SQL GIS extension queries.
 *
 * <p>SEE: ADR-007 (Distributed Rate Limiting & Idempotency using Redis)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RiderDispatchService {

    public static final String RIDERS_GEO_KEY = "riders:locations";
    private static final String RIDER_STATUS_PREFIX = "rider:status:";

    private final StringRedisTemplate redisTemplate;

    /**
     * Updates a rider's current GPS location and refreshes availability heartbeat.
     */
    public void updateLocation(UUID riderId, double latitude, double longitude) {
        log.debug("Updating rider location: riderId={}, lat={}, lon={}", riderId, latitude, longitude);

        // INVARIANT: Redis GEO takes longitude FIRST, then latitude (x, y coordinate convention).
        Point point = new Point(longitude, latitude);
        redisTemplate.opsForGeo().add(RIDERS_GEO_KEY, point, riderId.toString());

        // Refresh rider active heartbeat (expires in 10 minutes if no GPS ping)
        redisTemplate.opsForValue().set(RIDER_STATUS_PREFIX + riderId, "AVAILABLE", Duration.ofMinutes(10));
    }

    /**
     * Finds all active riders within the specified radius of coordinates, sorted by closest distance.
     */
    public List<NearbyRiderResponse> findNearbyRiders(double latitude, double longitude, double radiusKm) {
        log.info("Searching nearby riders: lat={}, lon={}, radiusKm={}", latitude, longitude, radiusKm);

        Point center = new Point(longitude, latitude);
        Distance radius = new Distance(radiusKm, Metrics.KILOMETERS);
        Circle searchArea = new Circle(center, radius);

        RedisGeoCommands.GeoRadiusCommandArgs args = RedisGeoCommands.GeoRadiusCommandArgs
                .newGeoRadiusArgs()
                .includeDistance()
                .includeCoordinates()
                .sortAscending();

        GeoResults<RedisGeoCommands.GeoLocation<String>> results = redisTemplate.opsForGeo()
                .radius(RIDERS_GEO_KEY, searchArea, args);

        if (results == null || results.getContent().isEmpty()) {
            return Collections.emptyList();
        }

        List<NearbyRiderResponse> riders = new ArrayList<>();
        results.getContent().forEach(geoResult -> {
            String riderIdStr = geoResult.getContent().getName();
            Point pt = geoResult.getContent().getPoint();
            double distance = geoResult.getDistance().getValue();

            // Check if rider is currently available
            String status = redisTemplate.opsForValue().get(RIDER_STATUS_PREFIX + riderIdStr);
            if ("AVAILABLE".equalsIgnoreCase(status)) {
                riders.add(new NearbyRiderResponse(
                        UUID.fromString(riderIdStr),
                        Math.round(distance * 100.0) / 100.0,
                        pt.getY(),
                        pt.getX()
                ));
            }
        });

        return riders;
    }

    /**
     * Automatically dispatches and locks the nearest available rider for an order.
     */
    public NearbyRiderResponse assignNearestRider(UUID orderId, double restaurantLat, double restaurantLon, double radiusKm) {
        List<NearbyRiderResponse> nearbyRiders = findNearbyRiders(restaurantLat, restaurantLon, radiusKm);

        if (nearbyRiders.isEmpty()) {
            throw new BusinessException("NO_RIDERS_AVAILABLE",
                    "No available delivery riders within " + radiusKm + " km of the restaurant.");
        }

        // Closest rider is first due to sortAscending
        NearbyRiderResponse selected = nearbyRiders.get(0);

        // Mark rider as ASSIGNED to avoid double-dispatch
        redisTemplate.opsForValue().set(RIDER_STATUS_PREFIX + selected.riderId(), "ASSIGNED", Duration.ofHours(2));
        log.info("Successfully dispatched riderId={} to orderId={}, distanceKm={}",
                selected.riderId(), orderId, selected.distanceKm());

        return selected;
    }
}
