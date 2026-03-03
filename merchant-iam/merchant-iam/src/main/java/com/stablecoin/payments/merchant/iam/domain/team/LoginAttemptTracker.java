package com.stablecoin.payments.merchant.iam.domain.team;

/**
 * Domain port for brute-force protection.
 * Tracks failed login attempts per email hash and enforces lockouts.
 */
public interface LoginAttemptTracker {

    /** Records a failed attempt. Returns the new total failure count. */
    int recordFailure(String emailHash);

    /** Returns true if the account is currently locked out. */
    boolean isLockedOut(String emailHash);

    /** Clears the failure counter on successful login. */
    void clearFailures(String emailHash);
}
