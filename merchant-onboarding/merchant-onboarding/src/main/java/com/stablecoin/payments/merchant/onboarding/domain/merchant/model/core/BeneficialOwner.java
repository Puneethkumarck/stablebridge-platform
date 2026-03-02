package com.stablecoin.payments.merchant.onboarding.domain.merchant.model.core;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDate;

@Builder(toBuilder = true)
public record BeneficialOwner(
        String fullName,
        LocalDate dateOfBirth,
        String nationality,
        BigDecimal ownershipPct,
        boolean isPoliticallyExposed,
        String nationalIdRef   // Vault reference, not the actual ID
) {}
