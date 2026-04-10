package com.stablecoin.payments.offramp.domain.exception;

import java.util.UUID;

public class PayoutPartnerException extends RuntimeException {

    public static final String ERROR_CODE = "FR-2003";

    public PayoutPartnerException(UUID payoutId, String partner, String reason) {
        super("Payout partner %s failed for payout %s: %s".formatted(partner, payoutId, reason));
    }

    public PayoutPartnerException(UUID payoutId, String partner, String reason, Throwable cause) {
        super("Payout partner %s failed for payout %s: %s".formatted(partner, payoutId, reason), cause);
    }

    public String errorCode() {
        return ERROR_CODE;
    }
}
