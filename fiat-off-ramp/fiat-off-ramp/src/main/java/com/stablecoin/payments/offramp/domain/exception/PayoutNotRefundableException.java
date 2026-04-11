package com.stablecoin.payments.offramp.domain.exception;

import java.util.UUID;

public class PayoutNotRefundableException extends RuntimeException {

    public static final String ERROR_CODE = "FR-2001";

    public PayoutNotRefundableException(UUID payoutId, String reason) {
        super("Payout order %s is not refundable: %s".formatted(payoutId, reason));
    }

    public String errorCode() {
        return ERROR_CODE;
    }
}
