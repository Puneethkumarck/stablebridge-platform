package com.stablecoin.payments.platform.infrastructure.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.config.MeterFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Shared metrics configuration for all services.
 *
 * <p>Registers common tags (service name, environment) on every meter,
 * ensuring consistent labelling across the platform for Prometheus / Grafana dashboards.
 *
 * <p>Activated by default; disable with {@code app.metrics.enabled=false}.
 */
@Configuration
@ConditionalOnBean(MeterRegistry.class)
@ConditionalOnProperty(name = "app.metrics.enabled", havingValue = "true", matchIfMissing = true)
public class MetricsConfig {

    @Bean
    MeterFilter commonTagsMeterFilter(
            @Value("${spring.application.name:unknown}") String serviceName,
            @Value("${app.environment:local}") String environment) {
        return MeterFilter.commonTags(List.of(
                Tag.of("service", serviceName),
                Tag.of("env", environment)
        ));
    }
}
