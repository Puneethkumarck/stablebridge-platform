package com.stablecoin.payments.merchant.iam.infrastructure.auth;

import com.nimbusds.jwt.SignedJWT;
import com.stablecoin.payments.merchant.iam.domain.team.model.MerchantUser;
import com.stablecoin.payments.merchant.iam.domain.team.model.Role;
import com.stablecoin.payments.merchant.iam.domain.team.model.core.AuthProvider;
import com.stablecoin.payments.merchant.iam.domain.team.model.core.BuiltInRole;
import com.stablecoin.payments.merchant.iam.domain.team.model.core.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class NimbusJwtTokenIssuerTest {

    private NimbusJwtTokenIssuer issuer;

    private final UUID merchantId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();
    private final UUID roleId = UUID.randomUUID();

    @BeforeEach
    void setUp() throws Exception {
        var props = new JwtProperties(
                null,  // blank = ephemeral key
                "https://test.example.com",
                "payment-platform",
                3600,
                86400);
        issuer = new NimbusJwtTokenIssuer(props);
        issuer.init();
    }

    private MerchantUser buildUser() {
        return MerchantUser.builder()
                .userId(userId).merchantId(merchantId)
                .email("admin@test.com").emailHash("hash")
                .fullName("Admin").status(UserStatus.ACTIVE)
                .roleId(roleId).authProvider(AuthProvider.LOCAL)
                .mfaEnabled(false)
                .createdAt(Instant.now()).updatedAt(Instant.now())
                .activatedAt(Instant.now())
                .build();
    }

    private Role buildRole() {
        return Role.builder()
                .roleId(roleId).merchantId(merchantId)
                .roleName("ADMIN").description("Admin")
                .builtin(true).active(true)
                .permissions(new ArrayList<>(BuiltInRole.ADMIN.defaultPermissions()))
                .createdAt(Instant.now()).updatedAt(Instant.now())
                .build();
    }

    @Test
    void issues_signed_access_token() throws Exception {
        var token = issuer.issueAccessToken(buildUser(), buildRole(), false);

        assertThat(token).isNotBlank();
        var jwt = SignedJWT.parse(token);
        assertThat(jwt.getHeader().getAlgorithm().getName()).isEqualTo("ES256");
    }

    @Test
    void access_token_contains_required_claims() throws Exception {
        var token = issuer.issueAccessToken(buildUser(), buildRole(), true);

        var jwt = SignedJWT.parse(token);
        var claims = jwt.getJWTClaimsSet();

        assertThat(claims.getSubject()).isEqualTo(userId.toString());
        assertThat(claims.getIssuer()).isEqualTo("https://test.example.com");
        assertThat(claims.getAudience()).contains("payment-platform");
        assertThat(claims.getStringClaim("merchant_id")).isEqualTo(merchantId.toString());
        assertThat(claims.getStringClaim("role")).isEqualTo("ADMIN");
        assertThat(claims.getBooleanClaim("mfa_verified")).isTrue();
        assertThat(claims.getStringListClaim("permissions")).isNotEmpty();
    }

    @Test
    void access_token_has_expiry() throws Exception {
        var token = issuer.issueAccessToken(buildUser(), buildRole(), false);
        var jwt = SignedJWT.parse(token);
        assertThat(jwt.getJWTClaimsSet().getExpirationTime()).isNotNull();
    }

    @Test
    void issues_signed_refresh_token() throws Exception {
        var token = issuer.issueRefreshToken(userId, UUID.randomUUID());
        assertThat(token).isNotBlank();
        var jwt = SignedJWT.parse(token);
        assertThat(jwt.getJWTClaimsSet().getSubject()).isEqualTo(userId.toString());
        assertThat(jwt.getJWTClaimsSet().getStringClaim("token_type")).isEqualTo("refresh");
    }

    @Test
    void jwks_json_contains_public_key() {
        var jwks = issuer.jwksJson();
        assertThat(jwks).contains("\"keys\"");
        assertThat(jwks).contains("\"EC\"");
        assertThat(jwks).contains("\"P-256\"");
        assertThat(jwks).contains("\"sig\"");
    }

    @Test
    void each_token_has_unique_jti() throws Exception {
        var t1 = SignedJWT.parse(issuer.issueAccessToken(buildUser(), buildRole(), false));
        var t2 = SignedJWT.parse(issuer.issueAccessToken(buildUser(), buildRole(), false));
        assertThat(t1.getJWTClaimsSet().getJWTID())
                .isNotEqualTo(t2.getJWTClaimsSet().getJWTID());
    }
}
