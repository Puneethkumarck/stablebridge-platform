package com.stablecoin.payments.platform.infrastructure.tracing;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.KafkaTemplate;

/**
 * Enables Micrometer Observation on Kafka producers and consumers so that trace context
 * (W3C {@code traceparent}) is automatically propagated through Kafka message headers.
 *
 * <p>This ensures end-to-end trace visibility across the event-driven pipeline:
 * S1 (Orchestrator) &rarr; Kafka &rarr; S2 (Compliance) &rarr; Kafka &rarr; S6 (FX) etc.
 *
 * <p>Uses a {@link BeanPostProcessor} to enable observation on all {@link KafkaTemplate}
 * and {@link ConcurrentKafkaListenerContainerFactory} instances, including those created
 * manually by individual services (not just auto-configured beans).
 *
 * <p>Activated only when both tracing and Kafka are on the classpath.
 */
@Slf4j
@Configuration
@ConditionalOnProperty(name = "app.tracing.enabled", havingValue = "true", matchIfMissing = true)
@ConditionalOnClass(KafkaTemplate.class)
public class KafkaTracingConfig {

    @Bean
    static KafkaObservationBeanPostProcessor kafkaObservationBeanPostProcessor() {
        return new KafkaObservationBeanPostProcessor();
    }

    /**
     * Post-processor that enables observation on all Kafka producer templates and
     * consumer container factories discovered in the application context.
     */
    static class KafkaObservationBeanPostProcessor implements BeanPostProcessor {

        @Override
        public Object postProcessAfterInitialization(Object bean, String beanName) {
            if (bean instanceof KafkaTemplate<?, ?> template) {
                template.setObservationEnabled(true);
                log.debug("Enabled observation on KafkaTemplate bean '{}'", beanName);
            }
            if (bean instanceof ConcurrentKafkaListenerContainerFactory<?, ?> factory) {
                factory.getContainerProperties().setObservationEnabled(true);
                log.debug("Enabled observation on KafkaListenerContainerFactory bean '{}'", beanName);
            }
            return bean;
        }
    }
}
