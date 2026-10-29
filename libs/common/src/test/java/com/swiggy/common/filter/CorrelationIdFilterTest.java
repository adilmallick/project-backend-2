package com.swiggy.common.filter;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests verifying the behavior of the CorrelationIdFilter.
 *
 * <p>WHAT THIS TEST PROVES:
 * 1. Generates a new UUID correlation ID if incoming request lacks one.
 * 2. Propagates existing correlation ID if caller provides one.
 * 3. Sets the X-Correlation-Id response header.
 * 4. Ensures MDC is cleaned up after request completes (no thread pollution).
 *
 * <p>WHICH FAILURE IT GUARDS AGAINST:
 * Guards against lost logs in distributed tracing and context leakage across thread pools.
 */
class CorrelationIdFilterTest {

    private final CorrelationIdFilter filter = new CorrelationIdFilter();

    @Test
    @DisplayName("Should generate a new correlation ID when none is provided in request headers")
    void shouldGenerateNewCorrelationIdWhenAbsent() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        filter.doFilter(request, response, filterChain);

        String generatedId = response.getHeader(CorrelationIdFilter.CORRELATION_ID_HEADER);
        assertThat(generatedId).isNotBlank();
        // INVARIANT: MDC must be clean after the request filter chain terminates
        assertThat(MDC.get(CorrelationIdFilter.CORRELATION_ID_MDC_KEY)).isNull();
    }

    @Test
    @DisplayName("Should preserve and propagate existing correlation ID provided in request headers")
    void shouldPreserveExistingCorrelationId() throws Exception {
        String existingCorrelationId = "test-correlation-uuid-12345";
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(CorrelationIdFilter.CORRELATION_ID_HEADER, existingCorrelationId);

        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        filter.doFilter(request, response, filterChain);

        assertThat(response.getHeader(CorrelationIdFilter.CORRELATION_ID_HEADER))
                .isEqualTo(existingCorrelationId);
        assertThat(MDC.get(CorrelationIdFilter.CORRELATION_ID_MDC_KEY)).isNull();
    }
}
