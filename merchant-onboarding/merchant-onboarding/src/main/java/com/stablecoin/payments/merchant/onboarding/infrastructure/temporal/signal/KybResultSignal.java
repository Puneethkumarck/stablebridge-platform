package com.stablecoin.payments.merchant.onboarding.infrastructure.temporal.signal;

import java.io.Serializable;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record KybResultSignal(UUID kybId, String provider, String providerRef, String status,
    Map<String, Object> riskSignals, String reviewNotes, Instant completedAt) implements Serializable {
}
