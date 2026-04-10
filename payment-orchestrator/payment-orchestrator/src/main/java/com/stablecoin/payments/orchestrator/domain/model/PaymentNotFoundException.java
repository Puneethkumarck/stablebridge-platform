package com.stablecoin.payments.orchestrator.domain.model;

import java.util.UUID;

public class PaymentNotFoundException extends RuntimeException {

    public PaymentNotFoundException(UUID paymentId) {
        super("Payment not found: " + paymentId);
    }
}
