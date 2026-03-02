package com.stablecoin.payments.merchant.iam.infrastructure.persistence;

import com.stablecoin.payments.merchant.iam.AbstractIntegrationTest;
import com.stablecoin.payments.merchant.iam.fixtures.IamEntityFixtures;
import com.stablecoin.payments.merchant.iam.infrastructure.persistence.repository.MerchantUserJpaRepository;
import com.stablecoin.payments.merchant.iam.infrastructure.persistence.repository.RoleJpaRepository;
import com.stablecoin.payments.merchant.iam.infrastructure.persistence.repository.UserSessionJpaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("UserSessionJpaRepository IT")
class UserSessionJpaRepositoryIT extends AbstractIntegrationTest {

    @Autowired
    private UserSessionJpaRepository userSessionJpaRepository;

    @Autowired
    private MerchantUserJpaRepository merchantUserJpaRepository;

    @Autowired
    private RoleJpaRepository roleJpaRepository;

    @Test
    @DisplayName("should find active sessions by user id")
    void shouldFindActiveSessions() {
        var role = IamEntityFixtures.anAdminRole();
        roleJpaRepository.save(role);

        var user = IamEntityFixtures.anActiveUser(role);
        merchantUserJpaRepository.save(user);

        var session = IamEntityFixtures.anActiveSession(user);
        userSessionJpaRepository.save(session);

        var sessions = userSessionJpaRepository.findByUser_UserIdAndRevokedFalse(user.getUserId());

        assertThat(sessions).hasSize(1);
        assertThat(sessions.getFirst().getIpAddress()).isEqualTo("192.168.1.1");
        assertThat(sessions.getFirst().isRevoked()).isFalse();
    }

    @Test
    @Transactional
    @DisplayName("should revoke all sessions by user id")
    void shouldRevokeByUser() {
        var role = roleJpaRepository.save(IamEntityFixtures.anAdminRole());

        var user = merchantUserJpaRepository.save(IamEntityFixtures.anActiveUser(role));

        var session1 = IamEntityFixtures.anActiveSession(user);
        var session2 = IamEntityFixtures.anActiveSession(user);
        userSessionJpaRepository.save(session1);
        userSessionJpaRepository.save(session2);

        int revoked = userSessionJpaRepository.revokeAllByUserId(user.getUserId(), "user_logout");

        assertThat(revoked).isEqualTo(2);
    }

    @Test
    @Transactional
    @DisplayName("should revoke all sessions by merchant id")
    void shouldRevokeByMerchant() {
        var role = roleJpaRepository.save(IamEntityFixtures.anAdminRole());

        var user = merchantUserJpaRepository.save(IamEntityFixtures.anActiveUser(role));

        var session = IamEntityFixtures.anActiveSession(user);
        userSessionJpaRepository.save(session);

        int revoked = userSessionJpaRepository.revokeAllByMerchantId(
                IamEntityFixtures.defaultMerchantId(), "merchant_suspended");

        assertThat(revoked).isEqualTo(1);
    }
}
