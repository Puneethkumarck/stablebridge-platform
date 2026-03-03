package com.stablecoin.payments.merchant.iam.application.controller;

import lombok.NoArgsConstructor;

import static lombok.AccessLevel.PRIVATE;

@NoArgsConstructor(access = PRIVATE)
public final class ErrorCodes {

    // 4xx — client errors
    public static final String BAD_REQUEST_CODE           = iamCode(1);
    public static final String USER_NOT_FOUND_CODE        = iamCode(2);
    public static final String ROLE_NOT_FOUND_CODE        = iamCode(3);
    public static final String INVITATION_NOT_FOUND_CODE  = iamCode(4);
    public static final String USER_ALREADY_EXISTS_CODE   = iamCode(5);
    public static final String LAST_ADMIN_CODE            = iamCode(6);
    public static final String ROLE_IN_USE_CODE           = iamCode(7);
    public static final String INVITATION_EXPIRED_CODE    = iamCode(8);
    public static final String BUILTIN_ROLE_MODIFY_CODE   = iamCode(9);
    public static final String INVALID_CREDENTIALS_CODE   = iamCode(10);
    public static final String INVALID_USER_STATE_CODE    = iamCode(11);
    public static final String MFA_REQUIRED_CODE          = iamCode(12);

    // 5xx — server errors
    public static final String INTERNAL_ERROR_CODE        = iamCode(50);

    static String iamCode(int code) {
        if (code > 9999) {
            throw new IllegalArgumentException("Cannot create error code with more than 4 digits");
        }
        return "IAM-%04d".formatted(code);
    }
}
