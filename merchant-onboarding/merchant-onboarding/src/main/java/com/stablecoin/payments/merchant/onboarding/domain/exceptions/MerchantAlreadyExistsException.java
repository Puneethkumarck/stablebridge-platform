package com.stablecoin.payments.merchant.onboarding.domain.exceptions;

public class MerchantAlreadyExistsException extends RuntimeException {

    private MerchantAlreadyExistsException(String message) {
        super(message);
    }

    public static MerchantAlreadyExistsException withRegistration(String regNumber, String country) {
        return new MerchantAlreadyExistsException(
                "Merchant already exists: registrationNumber=%s country=%s".formatted(regNumber, country));
    }
}
