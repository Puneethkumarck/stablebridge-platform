package com.stablecoin.payments.platform.infrastructure.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.config.MeterFilter;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

class MetricsConfigTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(MetricsConfig.class))
            .withUserConfiguration(TestMeterRegistryConfig.class);

    @Test
    @DisplayName("should register MeterFilter bean with correct common tags when metrics enabled")
    void shouldRegisterMeterFilterWithCommonTags() {
        contextRunner
                .withPropertyValues(
                        "app.metrics.enabled=true",
                        "spring.application.name=test-service",
                        "app.environment=test")
                .run(context -> {
                    assertThat(context).hasSingleBean(MetricsConfig.class);
                    var meterFilter = context.getBean(MeterFilter.class);
                    var registry = context.getBean(MeterRegistry.class);

                    // Apply the filter to the registry (Spring Boot's auto-config does this automatically)
                    registry.config().meterFilter(meterFilter);
                    registry.counter("test.counter").increment();

                    var counter = registry.find("test.counter").counter();
                    assertThat(counter).isNotNull();
                    assertThat(counter.getId().getTag("service")).isEqualTo("test-service");
                    assertThat(counter.getId().getTag("env")).isEqualTo("test");
                });
    }

    @Test
    @DisplayName("should produce MeterFilter with correct tags via bean method")
    void shouldProduceMeterFilterWithCorrectTags() {
        var config = new MetricsConfig();
        var filter = config.commonTagsMeterFilter("my-service", "staging");
        var registry = new SimpleMeterRegistry();

        registry.config().meterFilter(filter);
        registry.counter("verify.counter").increment();

        var counter = registry.find("verify.counter").counter();
        assertThat(counter).isNotNull();

        var tags = counter.getId().getTags();
        assertThat(tags).contains(Tag.of("service", "my-service"), Tag.of("env", "staging"));
    }

    @Test
    @DisplayName("should register MetricsConfig when property is absent (default)")
    void shouldRegisterMetricsConfigWhenPropertyAbsent() {
        contextRunner
                .withPropertyValues("spring.application.name=default-service")
                .run(context -> assertThat(context).hasSingleBean(MetricsConfig.class));
    }

    @Test
    @DisplayName("should not register MetricsConfig when metrics disabled")
    void shouldNotRegisterMetricsConfigWhenDisabled() {
        contextRunner
                .withPropertyValues("app.metrics.enabled=false")
                .run(context -> assertThat(context).doesNotHaveBean(MetricsConfig.class));
    }

    @Configuration
    static class TestMeterRegistryConfig {
        @Bean
        MeterRegistry meterRegistry() {
            return new SimpleMeterRegistry();
        }
    }
}
