package com.stablecoin.payments.merchant.onboarding.domain.merchant.model.core;

import lombok.Builder;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Builder(toBuilder = true)
public record KybVerification(
        UUID kybId,
        UUID merchantId,
        String provider,
        String providerRef,
        KybStatus status,
        Map<String, Object> riskSignals,
        List<DocumentType> documentsRequired,
        UUID reviewedBy,
        String reviewNotes,
        Instant initiatedAt,
        Instant completedAt
) {}
