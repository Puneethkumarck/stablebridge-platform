package com.stablecoin.payments.onramp.infrastructure.messaging;

import com.stablecoin.payments.onramp.domain.port.CollectionEventPublisher;
import com.stablecoin.payments.platform.infrastructure.messaging.AbstractOutboxEventPublisher;
import io.namastack.outbox.Outbox;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class OnRampOutboxEventPublisher extends AbstractOutboxEventPublisher
        implements CollectionEventPublisher {

    public OnRampOutboxEventPublisher(Outbox outbox) {
        super(outbox, List.of("paymentId", "collectionId"));
    }
}
