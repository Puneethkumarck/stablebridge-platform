package com.stablecoin.payments.platform.infrastructure.tracing;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.kafka.autoconfigure.KafkaAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.kafka.core.KafkaTemplate;

import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.assertThat;

class KafkaTracingConfigTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    KafkaAutoConfiguration.class,
                    KafkaTracingConfig.class))
            .withPropertyValues("spring.kafka.bootstrap-servers=localhost:9092");

    @Test
    @DisplayName("should enable observation on KafkaTemplate when tracing is enabled")
    void shouldEnableObservationOnKafkaTemplate() {
        contextRunner
                .withPropertyValues("app.tracing.enabled=true")
                .run(context -> {
                    assertThat(context).hasSingleBean(KafkaTemplate.class);
                    var template = context.getBean(KafkaTemplate.class);
                    assertThat(isObservationEnabled(template)).isTrue();
                });
    }

    @Test
    @DisplayName("should enable observation by default when app.tracing.enabled is absent")
    void shouldEnableObservationByDefault() {
        contextRunner
                .run(context -> {
                    assertThat(context).hasSingleBean(KafkaTemplate.class);
                    var template = context.getBean(KafkaTemplate.class);
                    assertThat(isObservationEnabled(template)).isTrue();
                });
    }

    @Test
    @DisplayName("should not enable observation when app.tracing.enabled is false")
    void shouldNotEnableObservationWhenTracingDisabled() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(
                        KafkaAutoConfiguration.class,
                        KafkaTracingConfig.class))
                .withPropertyValues(
                        "spring.kafka.bootstrap-servers=localhost:9092",
                        "app.tracing.enabled=false")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(KafkaTracingConfig.class);
                    assertThat(context).hasSingleBean(KafkaTemplate.class);
                    var template = context.getBean(KafkaTemplate.class);
                    assertThat(isObservationEnabled(template)).isFalse();
                });
    }

    @Test
    @DisplayName("should register BeanPostProcessor when tracing is enabled")
    void shouldRegisterBeanPostProcessor() {
        contextRunner
                .run(context ->
                        assertThat(context).hasSingleBean(
                                KafkaTracingConfig.KafkaObservationBeanPostProcessor.class));
    }

    @Test
    @DisplayName("should not register BeanPostProcessor when tracing is disabled")
    void shouldNotRegisterBeanPostProcessorWhenDisabled() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(
                        KafkaAutoConfiguration.class,
                        KafkaTracingConfig.class))
                .withPropertyValues(
                        "spring.kafka.bootstrap-servers=localhost:9092",
                        "app.tracing.enabled=false")
                .run(context ->
                        assertThat(context).doesNotHaveBean(
                                KafkaTracingConfig.KafkaObservationBeanPostProcessor.class));
    }

    /**
     * Reads the private {@code observationEnabled} field via reflection since
     * {@link KafkaTemplate} exposes a setter but no getter for this flag.
     */
    private static boolean isObservationEnabled(KafkaTemplate<?, ?> template) {
        try {
            Field field = KafkaTemplate.class.getDeclaredField("observationEnabled");
            field.setAccessible(true);
            return (boolean) field.get(template);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException("Failed to read observationEnabled field", e);
        }
    }
}
