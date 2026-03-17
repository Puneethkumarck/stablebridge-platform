package com.stablecoin.payments.offramp.infrastructure.messaging;

import com.stablecoin.payments.offramp.domain.port.PayoutEventPublisher;
import com.stablecoin.payments.platform.infrastructure.messaging.AbstractOutboxEventPublisher;
import io.namastack.outbox.Outbox;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class OffRampOutboxEventPublisher extends AbstractOutboxEventPublisher
        implements PayoutEventPublisher {

    public OffRampOutboxEventPublisher(Outbox outbox) {
        super(outbox, List.of("paymentId", "payoutId"));
    }
}
