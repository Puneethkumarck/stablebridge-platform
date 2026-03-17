package com.stablecoin.payments.custody.infrastructure.messaging;

import com.stablecoin.payments.custody.domain.port.TransferEventPublisher;
import com.stablecoin.payments.platform.infrastructure.messaging.AbstractOutboxEventPublisher;
import io.namastack.outbox.Outbox;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CustodyOutboxEventPublisher extends AbstractOutboxEventPublisher
        implements TransferEventPublisher {

    public CustodyOutboxEventPublisher(Outbox outbox) {
        super(outbox, List.of("paymentId", "transferId"));
    }
}
