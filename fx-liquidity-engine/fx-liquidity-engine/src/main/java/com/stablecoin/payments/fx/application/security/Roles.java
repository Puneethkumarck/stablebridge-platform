package com.stablecoin.payments.fx.application.security;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Roles {
    public static final String FX_READ = "fx:read";
    public static final String FX_WRITE = "fx:write";
    public static final String FX_ADMIN = "fx:admin";
}
