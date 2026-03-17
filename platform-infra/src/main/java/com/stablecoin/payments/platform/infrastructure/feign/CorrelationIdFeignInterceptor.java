package com.stablecoin.payments.platform.infrastructure.feign;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.slf4j.MDC;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.stereotype.Component;

/**
 * Feign {@link RequestInterceptor} that propagates the correlation ID from MDC
 * to outgoing HTTP requests via the {@code X-Correlation-Id} header.
 *
 * <p>Each service already has a {@code CorrelationIdFilter} that extracts (or generates)
 * the correlation ID from incoming requests and stores it in MDC under the key
 * {@code correlationId}. This interceptor closes the loop by forwarding that value
 * to downstream Feign calls, ensuring end-to-end traceability across service boundaries.
 *
 * <p>Activates only when Feign's {@link RequestInterceptor} is on the classpath.
 */
@Component
@ConditionalOnClass(RequestInterceptor.class)
public class CorrelationIdFeignInterceptor implements RequestInterceptor {

    static final String CORRELATION_ID_HEADER = "X-Correlation-Id";
    static final String CORRELATION_ID_MDC_KEY = "correlationId";

    @Override
    public void apply(RequestTemplate template) {
        String correlationId = MDC.get(CORRELATION_ID_MDC_KEY);
        if (correlationId != null) {
            template.header(CORRELATION_ID_HEADER, correlationId);
        }
    }
}
