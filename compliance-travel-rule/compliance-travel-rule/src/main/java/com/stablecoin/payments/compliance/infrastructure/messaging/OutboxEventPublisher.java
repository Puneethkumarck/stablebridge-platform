package com.stablecoin.payments.compliance.infrastructure.messaging;

import com.stablecoin.payments.compliance.domain.port.EventPublisher;
import com.stablecoin.payments.platform.infrastructure.messaging.AbstractOutboxEventPublisher;
import io.namastack.outbox.Outbox;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class OutboxEventPublisher extends AbstractOutboxEventPublisher
        implements EventPublisher<Object> {

    public OutboxEventPublisher(Outbox outbox) {
        super(outbox, List.of("paymentId"));
    }
}
