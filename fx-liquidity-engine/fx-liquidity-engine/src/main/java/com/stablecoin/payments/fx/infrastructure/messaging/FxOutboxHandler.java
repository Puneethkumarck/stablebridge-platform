package com.stablecoin.payments.fx.infrastructure.messaging;

import com.stablecoin.payments.platform.infrastructure.messaging.AbstractOutboxHandler;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class FxOutboxHandler extends AbstractOutboxHandler {

    public FxOutboxHandler(KafkaTemplate<String, Object> kafkaTemplate) {
        super(kafkaTemplate);
    }
}
