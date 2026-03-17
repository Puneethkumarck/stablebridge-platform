package com.stablecoin.payments.platform.infrastructure.feign;

import feign.RequestTemplate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import java.util.UUID;

import static com.stablecoin.payments.platform.infrastructure.feign.CorrelationIdFeignInterceptor.CORRELATION_ID_HEADER;
import static com.stablecoin.payments.platform.infrastructure.feign.CorrelationIdFeignInterceptor.CORRELATION_ID_MDC_KEY;
import static org.assertj.core.api.Assertions.assertThat;

class CorrelationIdFeignInterceptorTest {

    private final CorrelationIdFeignInterceptor interceptor = new CorrelationIdFeignInterceptor();

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    @DisplayName("should add correlation ID header when present in MDC")
    void shouldAddCorrelationIdHeaderWhenPresentInMdc() {
        var correlationId = UUID.randomUUID().toString();
        MDC.put(CORRELATION_ID_MDC_KEY, correlationId);
        var template = new RequestTemplate();

        interceptor.apply(template);

        assertThat(template.headers().get(CORRELATION_ID_HEADER)).containsExactly(correlationId);
    }

    @Test
    @DisplayName("should not add header when correlation ID absent from MDC")
    void shouldNotAddHeaderWhenCorrelationIdAbsentFromMdc() {
        var template = new RequestTemplate();

        interceptor.apply(template);

        assertThat(template.headers()).doesNotContainKey(CORRELATION_ID_HEADER);
    }

    @Test
    @DisplayName("should not overwrite existing correlation ID header")
    void shouldNotOverwriteExistingCorrelationIdHeader() {
        var existingId = UUID.randomUUID().toString();
        MDC.put(CORRELATION_ID_MDC_KEY, UUID.randomUUID().toString());
        var template = new RequestTemplate();
        template.header(CORRELATION_ID_HEADER, existingId);

        interceptor.apply(template);

        assertThat(template.headers().get(CORRELATION_ID_HEADER)).containsExactly(existingId);
    }
}
