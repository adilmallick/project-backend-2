package com.swiggy.user.ratelimit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.swiggy.common.dto.ApiResponse;
import com.swiggy.common.dto.ErrorDetails;
import com.swiggy.common.filter.CorrelationIdUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.Duration;
import java.util.Collections;

/**
 * Spring MVC HandlerInterceptor enforcing Redis Sliding-Window Rate Limiting.
 *
 * <p>Returns HTTP 429 (Too Many Requests) with RFC-compliant 'Retry-After' header
 * when a client exceeds the configured request threshold.
 *
 * <p>SEE: ADR-007 (Distributed Rate Limiting & Idempotency using Redis)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimitInterceptor implements HandlerInterceptor {

    private final RateLimiterService rateLimiterService;
    private final ObjectMapper objectMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        RateLimit rateLimit = handlerMethod.getMethodAnnotation(RateLimit.class);
        if (rateLimit == null) {
            return true;
        }

        String clientIp = extractClientIp(request);
        String rateLimitKey = clientIp + ":" + request.getRequestURI();

        boolean allowed = rateLimiterService.isAllowed(
                rateLimitKey,
                rateLimit.maxRequests(),
                Duration.ofSeconds(rateLimit.windowSeconds())
        );

        if (!allowed) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setHeader("Retry-After", String.valueOf(rateLimit.windowSeconds()));

            ErrorDetails errorDetails = new ErrorDetails(
                    "RATE_LIMIT_EXCEEDED",
                    "Too many requests. Please wait " + rateLimit.windowSeconds() + " seconds before retrying.",
                    Collections.emptyList()
            );
            ApiResponse<Void> apiResponse = ApiResponse.failure(errorDetails, CorrelationIdUtils.getCorrelationId());

            response.getWriter().write(objectMapper.writeValueAsString(apiResponse));
            return false;
        }

        return true;
    }

    private String extractClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
