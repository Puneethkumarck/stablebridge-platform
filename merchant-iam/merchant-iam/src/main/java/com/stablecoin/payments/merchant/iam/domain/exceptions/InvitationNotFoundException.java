package com.stablecoin.payments.merchant.iam.domain.exceptions;

import java.util.UUID;

public class InvitationNotFoundException extends RuntimeException {

    private InvitationNotFoundException(String message) {
        super(message);
    }

    public static InvitationNotFoundException withId(UUID invitationId) {
        return new InvitationNotFoundException("Invitation not found: " + invitationId);
    }

    public static InvitationNotFoundException withToken() {
        return new InvitationNotFoundException("Invitation not found for token");
    }
}
