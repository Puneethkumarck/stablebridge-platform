package com.stablecoin.payments.merchant.onboarding.domain.merchant.model.core;

import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

@Builder(toBuilder = true)
public record MerchantContact(
        UUID contactId,
        UUID merchantId,
        ContactRole role,
        String fullName,
        String email,
        String phone,
        boolean isActive,
        Instant createdAt
) {}
