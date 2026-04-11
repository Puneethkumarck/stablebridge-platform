package com.stablecoin.payments.platform.infrastructure.tracing;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "app.tracing.enabled", havingValue = "true", matchIfMissing = true)
public class TracingConfig {
}
