package com.stablecoin.payments.platform.infrastructure.feign;

import feign.RequestTemplate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CorrelationIdFeignInterceptorTest {

    private final CorrelationIdFeignInterceptor interceptor = new CorrelationIdFeignInterceptor();

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    void shouldAddCorrelationIdHeaderWhenPresentInMdc() {
        String correlationId = UUID.randomUUID().toString();
        MDC.put("correlationId", correlationId);

        RequestTemplate template = new RequestTemplate();
        interceptor.apply(template);

        Map<String, Collection<String>> headers = template.headers();
        assertThat(headers.get("X-Correlation-Id")).containsExactly(correlationId);
    }

    @Test
    void shouldNotAddHeaderWhenCorrelationIdAbsentFromMdc() {
        RequestTemplate template = new RequestTemplate();
        interceptor.apply(template);

        Map<String, Collection<String>> headers = template.headers();
        assertThat(headers).doesNotContainKey("X-Correlation-Id");
    }

    @Test
    void shouldNotOverwriteExistingCorrelationIdHeader() {
        String existingId = UUID.randomUUID().toString();
        String mdcId = UUID.randomUUID().toString();
        MDC.put("correlationId", mdcId);

        RequestTemplate template = new RequestTemplate();
        template.header("X-Correlation-Id", existingId);
        interceptor.apply(template);

        Map<String, Collection<String>> headers = template.headers();
        assertThat(headers.get("X-Correlation-Id")).contains(existingId).contains(mdcId);
    }
}
