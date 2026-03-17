package com.stablecoin.payments.orchestrator.infrastructure.messaging;

import com.stablecoin.payments.orchestrator.domain.port.PaymentEventPublisher;
import com.stablecoin.payments.platform.infrastructure.messaging.AbstractOutboxEventPublisher;
import io.namastack.outbox.Outbox;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class OutboxEventPublisher extends AbstractOutboxEventPublisher
        implements PaymentEventPublisher {

    public OutboxEventPublisher(Outbox outbox) {
        super(outbox, List.of("paymentId"));
    }
}
