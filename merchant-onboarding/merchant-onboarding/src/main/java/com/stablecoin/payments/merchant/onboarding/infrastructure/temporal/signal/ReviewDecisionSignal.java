package com.stablecoin.payments.merchant.onboarding.infrastructure.temporal.signal;

import java.io.Serializable;
import java.util.UUID;

public record ReviewDecisionSignal(String decision, String reason, UUID reviewedBy) implements Serializable {
}
