package com.stablecoin.payments.platform.infrastructure.tracing;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

/**
 * Enables distributed tracing across all services via Spring Boot auto-configuration.
 *
 * <p>When {@code app.tracing.enabled=true} (default), Spring Boot auto-configures:
 * <ul>
 *   <li>Micrometer Tracing bridge to OpenTelemetry ({@code micrometer-tracing-bridge-otel})</li>
 *   <li>OTLP HTTP span exporter to the collector configured via
 *       {@code management.otlp.tracing.endpoint}</li>
 *   <li>W3C {@code traceparent} header propagation on Feign/RestClient calls</li>
 *   <li>Trace context propagation through Kafka message headers when observation is enabled</li>
 *   <li>MDC population with {@code traceId} and {@code spanId} for structured logging</li>
 * </ul>
 *
 * <p>Set {@code app.tracing.enabled=false} in tests or environments without a collector.
 */
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
