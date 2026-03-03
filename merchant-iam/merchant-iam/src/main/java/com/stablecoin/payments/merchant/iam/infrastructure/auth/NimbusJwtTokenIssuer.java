package com.stablecoin.payments.merchant.iam.infrastructure.auth;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.stablecoin.payments.merchant.iam.domain.team.JwtTokenIssuer;
import com.stablecoin.payments.merchant.iam.domain.team.model.MerchantUser;
import com.stablecoin.payments.merchant.iam.domain.team.model.Role;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class NimbusJwtTokenIssuer implements JwtTokenIssuer {

    private final JwtProperties properties;

    private ECKey signingKey;
    private ECDSASigner signer;

    @PostConstruct
    void init() throws Exception {
        if (properties.privateKeyBase64() == null || properties.privateKeyBase64().isBlank()) {
            // Dev mode: generate ephemeral keypair
            signingKey = new ECKeyGenerator(Curve.P_256)
                    .keyUse(KeyUse.SIGNATURE)
                    .keyID(UUID.randomUUID().toString())
                    .algorithm(JWSAlgorithm.ES256)
                    .generate();
            log.warn("No JWT private key configured — using ephemeral key (dev mode only). " +
                     "Set merchant-iam.jwt.private-key-base64 for production.");
        } else {
            signingKey = (ECKey) ECKey.parseFromPEMEncodedObjects(
                    "-----BEGIN EC PRIVATE KEY-----\n" +
                    properties.privateKeyBase64() + "\n" +
                    "-----END EC PRIVATE KEY-----");
        }
        signer = new ECDSASigner(signingKey);
        log.info("JWT token issuer initialised keyId={}", signingKey.getKeyID());
    }

    @Override
    public String issueAccessToken(MerchantUser user, Role role, boolean mfaVerified) {
        try {
            var now = Instant.now();
            var expiry = now.plusSeconds(properties.accessTokenTtlSeconds());
            var permissions = role.permissions().stream()
                    .map(p -> p.namespace() + ":" + p.action())
                    .toList();

            var claims = new JWTClaimsSet.Builder()
                    .issuer(properties.issuer())
                    .subject(user.userId().toString())
                    .audience(properties.audience())
                    .jwtID(UUID.randomUUID().toString())
                    .issueTime(Date.from(now))
                    .expirationTime(Date.from(expiry))
                    .claim("merchant_id", user.merchantId().toString())
                    .claim("user_id", user.userId().toString())
                    .claim("role", role.roleName())
                    .claim("role_id", role.roleId().toString())
                    .claim("permissions", permissions)
                    .claim("mfa_verified", mfaVerified)
                    .build();

            var header = new JWSHeader.Builder(JWSAlgorithm.ES256)
                    .keyID(signingKey.getKeyID())
                    .build();

            var jwt = new SignedJWT(header, claims);
            jwt.sign(signer);
            return jwt.serialize();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to issue access token", e);
        }
    }

    @Override
    public String issueRefreshToken(UUID userId, UUID sessionId) {
        try {
            var now = Instant.now();
            var expiry = now.plusSeconds(properties.refreshTokenTtlSeconds());

            var claims = new JWTClaimsSet.Builder()
                    .issuer(properties.issuer())
                    .subject(userId.toString())
                    .jwtID(UUID.randomUUID().toString())
                    .issueTime(Date.from(now))
                    .expirationTime(Date.from(expiry))
                    .claim("session_id", sessionId.toString())
                    .claim("token_type", "refresh")
                    .build();

            var header = new JWSHeader.Builder(JWSAlgorithm.ES256)
                    .keyID(signingKey.getKeyID())
                    .build();

            var jwt = new SignedJWT(header, claims);
            jwt.sign(signer);
            return jwt.serialize();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to issue refresh token", e);
        }
    }

    @Override
    public String jwksJson() {
        try {
            var publicKey = signingKey.toPublicJWK();
            return new JWKSet(publicKey).toString();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to build JWKS", e);
        }
    }
}
