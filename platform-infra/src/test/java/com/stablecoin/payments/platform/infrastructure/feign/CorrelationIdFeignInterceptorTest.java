package com.stablecoin.payments.platform.infrastructure.feign;

import feign.RequestTemplate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import java.util.Collection;
import java.util.Map;
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
    void shouldAddCorrelationIdHeaderWhenPresentInMdc() {
        // Given
        String correlationId = UUID.randomUUID().toString();
        MDC.put(CORRELATION_ID_MDC_KEY, correlationId);
        RequestTemplate template = new RequestTemplate();

        // When
        interceptor.apply(template);

        // Then
        Map<String, Collection<String>> headers = template.headers();
        assertThat(headers.get(CORRELATION_ID_HEADER)).containsExactly(correlationId);
    }

    @Test
    void shouldNotAddHeaderWhenCorrelationIdAbsentFromMdc() {
        // Given
        RequestTemplate template = new RequestTemplate();

        // When
        interceptor.apply(template);

        // Then
        Map<String, Collection<String>> headers = template.headers();
        assertThat(headers).doesNotContainKey(CORRELATION_ID_HEADER);
    }

    @Test
    void shouldNotOverwriteExistingCorrelationIdHeader() {
        // Given
        String existingId = UUID.randomUUID().toString();
        String mdcId = UUID.randomUUID().toString();
        MDC.put(CORRELATION_ID_MDC_KEY, mdcId);
        RequestTemplate template = new RequestTemplate();
        template.header(CORRELATION_ID_HEADER, existingId);

        // When
        interceptor.apply(template);

        // Then
        Map<String, Collection<String>> headers = template.headers();
        assertThat(headers.get(CORRELATION_ID_HEADER)).containsExactly(existingId);
    }
}
