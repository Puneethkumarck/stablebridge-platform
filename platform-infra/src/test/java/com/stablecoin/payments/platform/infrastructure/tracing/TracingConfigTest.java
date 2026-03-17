package com.stablecoin.payments.platform.infrastructure.tracing;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class TracingConfigTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(TracingConfig.class));

    @Test
    @DisplayName("should register TracingConfig when app.tracing.enabled is true")
    void shouldRegisterTracingConfigWhenEnabled() {
        contextRunner
                .withPropertyValues("app.tracing.enabled=true")
                .run(context -> assertThat(context).hasSingleBean(TracingConfig.class));
    }

    @Test
    @DisplayName("should register TracingConfig when app.tracing.enabled property is absent (default)")
    void shouldRegisterTracingConfigWhenPropertyAbsent() {
        contextRunner
                .run(context -> assertThat(context).hasSingleBean(TracingConfig.class));
    }

    @Test
    @DisplayName("should not register TracingConfig when app.tracing.enabled is false")
    void shouldNotRegisterTracingConfigWhenDisabled() {
        contextRunner
                .withPropertyValues("app.tracing.enabled=false")
                .run(context -> assertThat(context).doesNotHaveBean(TracingConfig.class));
    }
}
