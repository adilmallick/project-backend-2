package com.swiggy.common.filter;

import org.slf4j.MDC;

import java.util.UUID;

/**
 * Utility to extract or generate the active correlation ID.
 */
public final class CorrelationIdUtils {

    private CorrelationIdUtils() {}

    public static String getCorrelationId() {
        String correlationId = MDC.get(CorrelationIdFilter.CORRELATION_ID_MDC_KEY);
        if (correlationId == null || correlationId.isBlank()) {
            return UUID.randomUUID().toString();
        }
        return correlationId;
    }
}
