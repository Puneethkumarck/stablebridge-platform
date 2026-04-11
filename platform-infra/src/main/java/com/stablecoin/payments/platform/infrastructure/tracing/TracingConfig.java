package com.stablecoin.payments.platform.infrastructure.tracing;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "app.tracing.enabled", havingValue = "true", matchIfMissing = true)
public class TracingConfig {

    // Spring Boot auto-configures all tracing beans (OtlpHttpSpanExporter, TracerProvider,
    // ContextPropagators) when micrometer-tracing-bridge-otel and opentelemetry-exporter-otlp
    // are on the classpath. No manual bean definitions needed.
    //
    // Key application properties driving behavior:
    //   management.tracing.sampling.probability  — sampling rate (1.0 = 100% in dev)
    //   management.otlp.tracing.endpoint         — OTLP collector HTTP endpoint
    //
    // This @Configuration class exists to:
    //   1. Provide a single toggle (app.tracing.enabled) to disable all tracing
    //   2. Document the tracing architecture for developers
    //   3. Serve as an extension point for custom SpanProcessor/SpanExporter beans
}
