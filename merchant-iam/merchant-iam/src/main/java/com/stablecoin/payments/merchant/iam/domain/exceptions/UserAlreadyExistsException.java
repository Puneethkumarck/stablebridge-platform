package com.stablecoin.payments.merchant.iam.domain.exceptions;

import java.util.UUID;

public class UserAlreadyExistsException extends RuntimeException {

    private UserAlreadyExistsException(String message) {
        super(message);
    }

    public static UserAlreadyExistsException forMerchant(UUID merchantId, String email) {
        return new UserAlreadyExistsException(
                "User already exists for merchant=%s email=%s".formatted(merchantId, email));
    }
}
