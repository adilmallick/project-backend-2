package com.swiggy.order.idempotency;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.swiggy.order.dto.OrderResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

/**
 * Distributed Idempotency Service using Redis SETNX (set-if-not-exists).
 *
 * <p>Prevents duplicate order creation and double-billing when client network retries or duplicate
 * clicks occur.
 *
 * <p>SEE: ADR-007 (Distributed Rate Limiting & Idempotency using Redis)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IdempotencyService {

    public static final String PROCESSING_SENTINEL = "__PROCESSING__";
    private static final String IDEMPOTENCY_PREFIX = "idempotency:order:";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    /**
     * Checks if a completed response has already been cached for this idempotency key.
     */
    public Optional<OrderResponse> getCachedResponse(String idempotencyKey) {
        String key = IDEMPOTENCY_PREFIX + idempotencyKey;
        String val = redisTemplate.opsForValue().get(key);

        if (val == null || PROCESSING_SENTINEL.equals(val)) {
            return Optional.empty();
        }

        try {
            OrderResponse response = objectMapper.readValue(val, OrderResponse.class);
            log.info("Idempotency cache HIT for key={}. Order id={}", idempotencyKey, response.id());
            return Optional.of(response);
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize cached OrderResponse for key={}: {}", idempotencyKey, e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Atomically acquires an in-flight processing lock using Redis SETNX.
     *
     * @return true if lock acquired; false if another request is already in progress or completed.
     */
    public boolean acquireLock(String idempotencyKey, Duration lockTtl) {
        String key = IDEMPOTENCY_PREFIX + idempotencyKey;
        Boolean acquired = redisTemplate.opsForValue().setIfAbsent(key, PROCESSING_SENTINEL, lockTtl);
        return Boolean.TRUE.equals(acquired);
    }

    /**
     * Stores the final order creation response in Redis, replacing the PROCESSING sentinel.
     */
    public void saveResponse(String idempotencyKey, OrderResponse response, Duration retentionTtl) {
        String key = IDEMPOTENCY_PREFIX + idempotencyKey;
        try {
            String json = objectMapper.writeValueAsString(response);
            redisTemplate.opsForValue().set(key, json, retentionTtl);
            log.info("Cached completed order response for idempotencyKey={}, orderId={}", idempotencyKey, response.id());
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize OrderResponse for key={}: {}", idempotencyKey, e.getMessage());
        }
    }

    /**
     * Releases the lock in case of business validation failure or error, allowing client retry.
     */
    public void releaseLock(String idempotencyKey) {
        String key = IDEMPOTENCY_PREFIX + idempotencyKey;
        redisTemplate.delete(key);
        log.warn("Released idempotency lock for key={}", idempotencyKey);
    }
}
