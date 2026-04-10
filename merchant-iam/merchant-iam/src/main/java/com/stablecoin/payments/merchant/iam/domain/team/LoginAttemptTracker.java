package com.stablecoin.payments.merchant.iam.domain.team;

public interface LoginAttemptTracker {

    int recordFailure(String emailHash);

    boolean isLockedOut(String emailHash);

    void clearFailures(String emailHash);
}
