package com.stablecoin.payments.merchant.iam.infrastructure.auth;

import com.stablecoin.payments.merchant.iam.domain.team.PasswordHasher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class BcryptPasswordHasher implements PasswordHasher {

    private static final int COST = 12;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(COST);

    @Override
    public String hash(String rawPassword) {
        return encoder.encode(rawPassword);
    }

    @Override
    public boolean verify(String rawPassword, String hashedPassword) {
        return encoder.matches(rawPassword, hashedPassword);
    }
}
