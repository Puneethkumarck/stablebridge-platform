package com.stablecoin.payments.merchant.onboarding.infrastructure.messaging;

import com.stablecoin.payments.platform.infrastructure.messaging.AbstractOutboxHandler;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class OnboardingOutboxHandler extends AbstractOutboxHandler {

    public OnboardingOutboxHandler(KafkaTemplate<String, Object> kafkaTemplate) {
        super(kafkaTemplate);
    }
}
