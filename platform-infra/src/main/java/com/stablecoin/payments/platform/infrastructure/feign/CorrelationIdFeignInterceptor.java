package com.stablecoin.payments.platform.infrastructure.feign;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.slf4j.MDC;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnClass(RequestInterceptor.class)
public class CorrelationIdFeignInterceptor implements RequestInterceptor {

    static final String CORRELATION_ID_HEADER = "X-Correlation-Id";
    static final String CORRELATION_ID_MDC_KEY = "correlationId";

    @Override
    public void apply(RequestTemplate template) {
        var correlationId = MDC.get(CORRELATION_ID_MDC_KEY);
        if (correlationId != null && !template.headers().containsKey(CORRELATION_ID_HEADER)) {
            template.header(CORRELATION_ID_HEADER, correlationId);
        }
    }
}
