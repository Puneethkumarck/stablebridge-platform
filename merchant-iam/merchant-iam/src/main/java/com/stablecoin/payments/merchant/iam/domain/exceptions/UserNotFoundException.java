package com.stablecoin.payments.merchant.iam.domain.exceptions;

import java.util.UUID;

public class UserNotFoundException extends RuntimeException {

    private UserNotFoundException(String message) {
        super(message);
    }

    public static UserNotFoundException withId(UUID userId) {
        return new UserNotFoundException("User not found: " + userId);
    }

    public static UserNotFoundException withEmailHash(UUID merchantId, String emailHash) {
        return new UserNotFoundException(
                "User not found for merchant=%s emailHash=%s".formatted(merchantId, emailHash));
    }
}
