package com.stablecoin.payments.merchant.iam.domain.exceptions;

import java.util.UUID;

public class InvitationExpiredException extends RuntimeException {

    private InvitationExpiredException(String message) {
        super(message);
    }

    public static InvitationExpiredException withId(UUID invitationId) {
        return new InvitationExpiredException("Invitation has expired: " + invitationId);
    }
}
