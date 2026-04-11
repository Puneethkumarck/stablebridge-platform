package com.stablecoin.payments.merchant.iam.domain.team;

public interface PasswordHasher {

    String hash(String rawPassword);

    boolean verify(String rawPassword, String hashedPassword);
}
