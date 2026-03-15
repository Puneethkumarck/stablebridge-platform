package com.stablecoin.payments.fx.domain.exception;

import java.util.UUID;

public class LockNotFoundException extends RuntimeException {

    private LockNotFoundException(String message) {
        super(message);
    }

    public static LockNotFoundException withId(UUID lockId) {
        return new LockNotFoundException("Lock not found: " + lockId);
    }
}
