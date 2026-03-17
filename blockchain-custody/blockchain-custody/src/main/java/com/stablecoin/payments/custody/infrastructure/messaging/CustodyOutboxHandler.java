package com.stablecoin.payments.custody.infrastructure.messaging;

import com.stablecoin.payments.platform.infrastructure.messaging.AbstractOutboxHandler;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class CustodyOutboxHandler extends AbstractOutboxHandler {

    public CustodyOutboxHandler(KafkaTemplate<String, Object> kafkaTemplate) {
        super(kafkaTemplate);
    }
}
