package com.stablecoin.payments.merchant.iam.infrastructure.auth;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BcryptPasswordHasherTest {

    private final BcryptPasswordHasher hasher = new BcryptPasswordHasher();

    @Test
    void hash_produces_bcrypt_string() {
        var hashed = hasher.hash("SecureP@ssw0rd!");
        assertThat(hashed).startsWith("$2a$12$");
    }

    @Test
    void verify_returns_true_for_matching_password() {
        var raw = "SecureP@ssw0rd!";
        var hashed = hasher.hash(raw);
        assertThat(hasher.verify(raw, hashed)).isTrue();
    }

    @Test
    void verify_returns_false_for_wrong_password() {
        var hashed = hasher.hash("correct-password");
        assertThat(hasher.verify("wrong-password", hashed)).isFalse();
    }

    @Test
    void same_password_produces_different_hashes() {
        var raw = "SamePassword123!";
        assertThat(hasher.hash(raw)).isNotEqualTo(hasher.hash(raw));
    }
}
