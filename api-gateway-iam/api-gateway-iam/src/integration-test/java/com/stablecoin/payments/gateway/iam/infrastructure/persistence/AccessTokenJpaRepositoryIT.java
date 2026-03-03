package com.stablecoin.payments.gateway.iam.infrastructure.persistence;

import com.stablecoin.payments.gateway.iam.AbstractIntegrationTest;
import com.stablecoin.payments.gateway.iam.fixtures.GatewayEntityFixtures;
import com.stablecoin.payments.gateway.iam.infrastructure.persistence.repository.AccessTokenJpaRepository;
import com.stablecoin.payments.gateway.iam.infrastructure.persistence.repository.MerchantJpaRepository;
import com.stablecoin.payments.gateway.iam.infrastructure.persistence.repository.OAuthClientJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("AccessTokenJpaRepository IT")
class AccessTokenJpaRepositoryIT extends AbstractIntegrationTest {

    @Autowired
    private AccessTokenJpaRepository tokenRepository;

    @Autowired
    private MerchantJpaRepository merchantRepository;

    @Autowired
    private OAuthClientJpaRepository clientRepository;

    private UUID merchantId;
    private UUID clientId;

    @BeforeEach
    void setUpMerchantAndClient() {
        var merchant = GatewayEntityFixtures.anActiveMerchant();
        merchantRepository.save(merchant);
        merchantId = merchant.getMerchantId();

        var client = GatewayEntityFixtures.anActiveOAuthClient(merchantId);
        clientRepository.save(client);
        clientId = client.getClientId();
    }

    @Test
    @DisplayName("should save and find token by jti")
    void shouldSaveAndFindByJti() {
        var token = GatewayEntityFixtures.anActiveAccessToken(merchantId, clientId);
        tokenRepository.save(token);

        var found = tokenRepository.findById(token.getJti());

        assertThat(found).isPresent();
        assertThat(found.get().getMerchantId()).isEqualTo(merchantId);
        assertThat(found.get().getClientId()).isEqualTo(clientId);
        assertThat(found.get().isRevoked()).isFalse();
    }

    @Test
    @Transactional
    @DisplayName("should revoke all tokens by merchant id")
    void shouldRevokeAllByMerchantId() {
        var token1 = GatewayEntityFixtures.anActiveAccessToken(merchantId, clientId);
        var token2 = GatewayEntityFixtures.anActiveAccessToken(merchantId, clientId);
        tokenRepository.save(token1);
        tokenRepository.save(token2);

        int revoked = tokenRepository.revokeAllByMerchantId(merchantId);

        assertThat(revoked).isEqualTo(2);
    }

    @Test
    @Transactional
    @DisplayName("should delete expired revoked tokens before cutoff")
    void shouldDeleteExpiredBefore() {
        var expiredToken = GatewayEntityFixtures.anActiveAccessToken(merchantId, clientId);
        expiredToken.setRevoked(true);
        expiredToken.setRevokedAt(Instant.now().minusSeconds(7200));
        expiredToken.setExpiresAt(Instant.now().minusSeconds(3600));
        tokenRepository.save(expiredToken);

        var activeToken = GatewayEntityFixtures.anActiveAccessToken(merchantId, clientId);
        tokenRepository.save(activeToken);

        int deleted = tokenRepository.deleteExpiredBefore(Instant.now());

        assertThat(deleted).isEqualTo(1);
        assertThat(tokenRepository.findById(activeToken.getJti())).isPresent();
    }

    @Test
    @DisplayName("should persist scopes as text array")
    void shouldPersistScopesAsTextArray() {
        var token = GatewayEntityFixtures.anActiveAccessToken(merchantId, clientId);
        tokenRepository.save(token);

        var found = tokenRepository.findById(token.getJti()).orElseThrow();

        assertThat(found.getScopes()).containsExactly("payments:read");
    }
}
