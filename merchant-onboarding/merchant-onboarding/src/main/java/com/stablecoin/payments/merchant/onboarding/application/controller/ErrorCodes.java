package com.stablecoin.payments.merchant.onboarding.application.controller;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ErrorCodes {

    private static final String BASE_URI = "https://stablecoin.payments/errors/";

    public static final String MERCHANT_NOT_FOUND = BASE_URI + "merchant-not-found";
    public static final String MERCHANT_ALREADY_EXISTS = BASE_URI + "merchant-already-exists";
    public static final String INVALID_STATE = BASE_URI + "invalid-state";
    public static final String VALIDATION_ERROR = BASE_URI + "validation-error";
    public static final String INTERNAL_ERROR = BASE_URI + "internal-error";
}
