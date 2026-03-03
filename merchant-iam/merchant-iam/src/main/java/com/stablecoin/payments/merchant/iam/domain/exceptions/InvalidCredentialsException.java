package com.stablecoin.payments.merchant.iam.domain.exceptions;

public class InvalidCredentialsException extends RuntimeException {

    private InvalidCredentialsException(String message) {
        super(message);
    }

    public static InvalidCredentialsException invalidEmailOrPassword() {
        return new InvalidCredentialsException("Invalid email or password");
    }
}
