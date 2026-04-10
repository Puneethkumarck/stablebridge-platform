package com.stablecoin.payments.merchant.iam.domain.team;

import java.util.Optional;
import java.util.UUID;

public interface MfaChallengeStore {

    record Challenge(UUID userId, UUID merchantId, String emailHash) {}

    String store(UUID userId, UUID merchantId, String emailHash);

    Optional<Challenge> consume(String challengeId);
}
