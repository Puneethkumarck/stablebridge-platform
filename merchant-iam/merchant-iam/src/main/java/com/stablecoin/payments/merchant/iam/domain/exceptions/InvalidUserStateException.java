package com.stablecoin.payments.merchant.iam.domain.exceptions;

import java.util.UUID;

public class InvalidUserStateException extends RuntimeException {

    private InvalidUserStateException(String message) {
        super(message);
    }

    public static InvalidUserStateException forUser(UUID userId, String currentState, String attemptedAction) {
        return new InvalidUserStateException(
                "Cannot %s user=%s in state %s".formatted(attemptedAction, userId, currentState));
    }
}
