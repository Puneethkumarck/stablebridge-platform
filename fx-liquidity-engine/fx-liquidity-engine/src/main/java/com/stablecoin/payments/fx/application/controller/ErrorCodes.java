package com.stablecoin.payments.fx.application.controller;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ErrorCodes {
    public static final String VALIDATION_ERROR = "FX-0001";
    public static final String QUOTE_NOT_FOUND = "FX-1001";
    public static final String QUOTE_EXPIRED = "FX-1002";
    public static final String QUOTE_ALREADY_LOCKED = "FX-1003";
    public static final String LOCK_NOT_FOUND = "FX-2001";
    public static final String INSUFFICIENT_LIQUIDITY = "FX-3001";
    public static final String CORRIDOR_NOT_SUPPORTED = "FX-3002";
    public static final String RATE_UNAVAILABLE = "FX-4001";
    public static final String INTERNAL_ERROR = "FX-9999";
}
