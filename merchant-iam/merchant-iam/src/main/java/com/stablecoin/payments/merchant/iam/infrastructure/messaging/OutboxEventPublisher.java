package com.stablecoin.payments.merchant.iam.infrastructure.messaging;

import com.stablecoin.payments.merchant.iam.domain.EventPublisher;
import com.stablecoin.payments.platform.infrastructure.messaging.AbstractOutboxEventPublisher;
import io.namastack.outbox.Outbox;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class OutboxEventPublisher extends AbstractOutboxEventPublisher
        implements EventPublisher<Object> {

    public OutboxEventPublisher(Outbox outbox) {
        super(outbox, List.of("merchantId"));
    }
}
