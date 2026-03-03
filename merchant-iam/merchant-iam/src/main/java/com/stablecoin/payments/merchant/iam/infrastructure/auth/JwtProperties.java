package com.stablecoin.payments.merchant.iam.infrastructure.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "merchant-iam.jwt")
public record JwtProperties(
        /** Base64-encoded PKCS#8 ES256 private key (PEM without headers). Dev: generated on startup. */
        String privateKeyBase64,
        /** Issuer claim (iss) */
        @NotBlank String issuer,
        /** Audience claim (aud) */
        @NotBlank String audience,
        /** Access token TTL in seconds */
        @Positive int accessTokenTtlSeconds,
        /** Refresh token TTL in seconds */
        @Positive int refreshTokenTtlSeconds
) {}
