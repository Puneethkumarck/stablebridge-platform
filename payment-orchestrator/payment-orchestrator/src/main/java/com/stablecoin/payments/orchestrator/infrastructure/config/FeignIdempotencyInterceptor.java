package com.stablecoin.payments.orchestrator.infrastructure.config;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class FeignIdempotencyInterceptor implements RequestInterceptor {

    private static final String IDEMPOTENCY_KEY_HEADER = "Idempotency-Key";

    @Override
    public void apply(RequestTemplate template) {
        if (!template.headers().containsKey(IDEMPOTENCY_KEY_HEADER)) {
            template.header(IDEMPOTENCY_KEY_HEADER, UUID.randomUUID().toString());
        }
    }
}
