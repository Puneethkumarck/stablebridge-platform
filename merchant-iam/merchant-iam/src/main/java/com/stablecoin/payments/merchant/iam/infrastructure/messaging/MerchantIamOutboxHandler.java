package com.stablecoin.payments.merchant.iam.infrastructure.messaging;

import com.stablecoin.payments.platform.infrastructure.messaging.AbstractOutboxHandler;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class MerchantIamOutboxHandler extends AbstractOutboxHandler {

    public MerchantIamOutboxHandler(KafkaTemplate<String, Object> kafkaTemplate) {
        super(kafkaTemplate);
    }
}
