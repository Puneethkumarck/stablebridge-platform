package com.stablecoin.payments.custody.domain.exception;

public class ChainUnavailableException extends RuntimeException {

    public static final String ERROR_CODE = "BC-1002";

    public ChainUnavailableException(String message) {
        super(message);
    }
}
