package com.stablecoin.payments.platform.infrastructure.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.security.headers")
public record SecurityHeadersProperties(
        Boolean enabled,
        Boolean hstsEnabled,
        Long hstsMaxAge
) {

    private static final boolean DEFAULT_ENABLED = true;
    private static final boolean DEFAULT_HSTS_ENABLED = true;
    private static final long DEFAULT_HSTS_MAX_AGE = 31_536_000L;

    public SecurityHeadersProperties {
        if (enabled == null) {
            enabled = DEFAULT_ENABLED;
        }
        if (hstsEnabled == null) {
            hstsEnabled = DEFAULT_HSTS_ENABLED;
        }
        if (hstsMaxAge == null) {
            hstsMaxAge = DEFAULT_HSTS_MAX_AGE;
        }
    }
}
