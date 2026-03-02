package com.stablecoin.payments.merchant.onboarding.domain.exceptions;

import com.stablecoin.payments.merchant.onboarding.domain.merchant.model.core.MerchantStatus;

import java.util.UUID;

public class InvalidMerchantStateException extends RuntimeException {

    private InvalidMerchantStateException(String message) {
        super(message);
    }

    public static InvalidMerchantStateException forMerchant(UUID merchantId, MerchantStatus currentStatus, String operation) {
        return new InvalidMerchantStateException(
                "Cannot perform '%s' on merchant=%s in status=%s".formatted(operation, merchantId, currentStatus));
    }
}
