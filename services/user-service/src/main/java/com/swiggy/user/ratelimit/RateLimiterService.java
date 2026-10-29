package com.swiggy.user.ratelimit;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

/**
 * Distributed Sliding-Window Rate Limiter powered by Redis Sorted Sets (ZSET).
 *
 * <p>DECISION: Sliding-Window log with ZSET eliminates boundary burst vulnerabilities present
 * in Fixed-Window and Simple Token Bucket implementations.
 *
 * <p>TRADE-OFF: ZSET requires O(log N + M) where M is expired items. For typical rate windows
 * (e.g. 5 requests per 10s), memory and CPU overhead is negligible (< 100 microseconds per request).
 *
 * <p>SEE: ADR-007 (Distributed Rate Limiting & Idempotency using Redis)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RateLimiterService {

    private final StringRedisTemplate redisTemplate;

    /**
     * Checks if a request identified by key is allowed within the sliding window.
     *
     * @param key unique identifier (e.g. client IP or username)
     * @param maxRequests maximum permitted requests in the window
     * @param window duration of the sliding window
     * @return true if permitted, false if rate limit exceeded
     */
    public boolean isAllowed(String key, int maxRequests, Duration window) {
        String redisKey = "rate_limit:" + key;
        long now = System.currentTimeMillis();
        long windowStart = now - window.toMillis();

        try {
            ZSetOperations<String, String> zset = redisTemplate.opsForZSet();

            // 1. Evict entries older than the sliding window boundary
            zset.removeRangeByScore(redisKey, 0, windowStart);

            // 2. Count requests currently within the window
            Long currentRequests = zset.zCard(redisKey);

            if (currentRequests != null && currentRequests >= maxRequests) {
                log.warn("Rate limit EXCEEDED for key='{}': {}/{} requests in {}s",
                        key, currentRequests, maxRequests, window.toSeconds());
                return false;
            }

            // 3. Record the current request with unique value to avoid ZSET member deduplication
            String member = now + ":" + UUID.randomUUID().toString().substring(0, 8);
            zset.add(redisKey, member, now);

            // 4. Set TTL on the set slightly longer than the window so unused keys expire automatically
            redisTemplate.expire(redisKey, window.plusSeconds(5));

            return true;
        } catch (Exception e) {
            // DECISION: Fail-open strategy. If Redis is unavailable, log warning and allow the request
            // to avoid taking down authentication during cache/redis outages.
            log.error("Redis error in RateLimiterService for key='{}'. Failing open. Error: {}", key, e.getMessage());
            return true;
        }
    }
}
