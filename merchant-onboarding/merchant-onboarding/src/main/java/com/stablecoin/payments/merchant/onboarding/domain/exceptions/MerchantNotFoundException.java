package com.stablecoin.payments.merchant.onboarding.domain.exceptions;

import java.util.UUID;

public class MerchantNotFoundException extends RuntimeException {

    private MerchantNotFoundException(String message) {
        super(message);
    }

    public static MerchantNotFoundException withId(UUID merchantId) {
        return new MerchantNotFoundException("No merchant found with id=%s".formatted(merchantId));
    }
}
