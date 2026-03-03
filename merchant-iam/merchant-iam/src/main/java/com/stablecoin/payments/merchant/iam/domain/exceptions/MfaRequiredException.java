package com.stablecoin.payments.merchant.iam.domain.exceptions;

import java.util.UUID;

public class MfaRequiredException extends RuntimeException {

    private final UUID userId;
    private final UUID sessionId;

    private MfaRequiredException(String message, UUID userId, UUID sessionId) {
        super(message);
        this.userId = userId;
        this.sessionId = sessionId;
    }

    public static MfaRequiredException forUser(UUID userId, UUID sessionId) {
        return new MfaRequiredException(
                "MFA verification required for user: " + userId, userId, sessionId);
    }

    public UUID getUserId() {
        return userId;
    }

    public UUID getSessionId() {
        return sessionId;
    }
}
