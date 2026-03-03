package com.stablecoin.payments.merchant.iam.domain.team;

import java.util.Optional;
import java.util.UUID;

/**
 * Domain port for storing short-lived MFA challenges.
 * A challenge maps a challengeId → (userId, merchantId) with a 5-minute TTL.
 */
public interface MfaChallengeStore {

    record Challenge(UUID userId, UUID merchantId, String emailHash) {}

    /** Stores a challenge and returns the generated challengeId. */
    String store(UUID userId, UUID merchantId, String emailHash);

    /** Looks up and removes a challenge by its ID. Returns empty if expired or not found. */
    Optional<Challenge> consume(String challengeId);
}
