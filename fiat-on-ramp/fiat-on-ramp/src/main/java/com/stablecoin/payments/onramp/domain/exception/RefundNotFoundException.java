package com.stablecoin.payments.onramp.domain.exception;

import java.util.UUID;

public class RefundNotFoundException extends RuntimeException {

    public static final String ERROR_CODE = "OR-2001";

    public RefundNotFoundException(UUID refundId) {
        super("Refund not found: " + refundId);
    }

    public String errorCode() {
        return ERROR_CODE;
    }
}
