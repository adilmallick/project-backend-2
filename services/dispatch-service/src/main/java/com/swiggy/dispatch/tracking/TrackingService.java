package com.swiggy.dispatch.tracking;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.swiggy.common.exception.BusinessException;
import com.swiggy.dispatch.dto.TrackingLocationPayload;
import com.swiggy.dispatch.service.RiderDispatchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * Service orchestrating real-time GPS telemetry, Redis GEO persistence,
 * and Redis Pub/Sub broadcast across the cluster.
 *
 * <p>SEE: ADR-009 (Real-Time Location Streaming with STOMP & Redis Backplane)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TrackingService {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    /**
     * Publishes a live tracking update:
     * 1. Updates rider coordinates in Redis Geospatial index.
     * 2. Broadcasts JSON payload to Redis Pub/Sub channel 'tracking:orders:{orderId}'.
     */
    public void publishTrackingUpdate(TrackingLocationPayload payload) {
        log.info("Publishing tracking update: orderId={}, riderId={}, lat={}, lon={}",
                payload.orderId(), payload.riderId(), payload.latitude(), payload.longitude());

        // 1. Update rider in Redis GEO index
        Point point = new Point(payload.longitude(), payload.latitude());
        redisTemplate.opsForGeo().add(RiderDispatchService.RIDERS_GEO_KEY, point, payload.riderId().toString());

        // 2. Publish to Redis Pub/Sub backplane channel
        String channel = "tracking:orders:" + payload.orderId();
        try {
            String json = objectMapper.writeValueAsString(payload);
            redisTemplate.convertAndSend(channel, json);
        } catch (JsonProcessingException e) {
            throw new BusinessException("TRACKING_SERIALIZATION_FAILED", "Failed to serialize tracking payload: " + e.getMessage());
        }
    }
}
