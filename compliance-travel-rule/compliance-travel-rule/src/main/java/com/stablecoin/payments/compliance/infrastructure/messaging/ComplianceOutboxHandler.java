package com.stablecoin.payments.compliance.infrastructure.messaging;

import com.stablecoin.payments.platform.infrastructure.messaging.AbstractOutboxHandler;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class ComplianceOutboxHandler extends AbstractOutboxHandler {

    public ComplianceOutboxHandler(KafkaTemplate<String, Object> kafkaTemplate) {
        super(kafkaTemplate);
    }
}
