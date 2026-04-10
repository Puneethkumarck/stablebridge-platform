package com.stablecoin.payments.merchant.iam.domain.team;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@Slf4j
@Component
public class EmailHasher {

    public String hash(String email) {
        try {
            var normalised = email.trim().toLowerCase();
            var digest = MessageDigest.getInstance("SHA-256");
            var bytes = digest.digest(normalised.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
