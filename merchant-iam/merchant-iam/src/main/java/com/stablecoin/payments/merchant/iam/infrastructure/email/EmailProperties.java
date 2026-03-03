package com.stablecoin.payments.merchant.iam.infrastructure.email;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "merchant-iam.email")
public record EmailProperties(String from, String invitationBaseUrl) {
}
