package com.stablecoin.payments.platform.infrastructure.tracing;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.KafkaTemplate;

@Slf4j
@Configuration
@ConditionalOnProperty(name = "app.tracing.enabled", havingValue = "true", matchIfMissing = true)
@ConditionalOnClass(KafkaTemplate.class)
public class KafkaTracingConfig {

    @Bean
    static KafkaObservationBeanPostProcessor kafkaObservationBeanPostProcessor() {
        return new KafkaObservationBeanPostProcessor();
    }

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
