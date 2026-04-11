package com.stablecoin.payments.orchestrator.domain.model;

import java.util.UUID;

public class PaymentNotCancellableException extends RuntimeException {

    public PaymentNotCancellableException(UUID paymentId, PaymentState state) {
        super("Payment %s cannot be cancelled in state %s".formatted(paymentId, state));
    }
}
