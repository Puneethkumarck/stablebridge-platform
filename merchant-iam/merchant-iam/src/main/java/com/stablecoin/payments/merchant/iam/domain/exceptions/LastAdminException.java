package com.stablecoin.payments.merchant.iam.domain.exceptions;

import java.util.UUID;

public class LastAdminException extends RuntimeException {

    private LastAdminException(String message) {
        super(message);
    }

    public static LastAdminException forMerchant(UUID merchantId) {
        return new LastAdminException(
                "Cannot remove or demote the last admin for merchant: " + merchantId);
    }
}
