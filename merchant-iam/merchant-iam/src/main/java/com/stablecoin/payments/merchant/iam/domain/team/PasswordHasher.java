package com.stablecoin.payments.merchant.iam.domain.team;

/**
 * Domain port for password hashing and verification.
 * Infrastructure provides a bcrypt implementation (cost 12).
 */
public interface PasswordHasher {

    String hash(String rawPassword);

    boolean verify(String rawPassword, String hashedPassword);
}
