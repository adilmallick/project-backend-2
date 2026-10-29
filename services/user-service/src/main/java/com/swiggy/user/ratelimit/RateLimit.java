package com.swiggy.user.ratelimit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to enforce Redis Sliding-Window Rate Limiting on controller endpoints.
 *
 * <p>SEE: ADR-007 (Distributed Rate Limiting & Idempotency using Redis)
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {

    /**
     * Maximum number of requests allowed within the time window.
     */
    int maxRequests() default 5;

    /**
     * Sliding window duration in seconds.
     */
    int windowSeconds() default 10;
}
