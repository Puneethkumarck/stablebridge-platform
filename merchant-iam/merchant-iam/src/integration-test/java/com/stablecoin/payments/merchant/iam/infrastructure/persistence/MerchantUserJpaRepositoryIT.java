package com.stablecoin.payments.merchant.iam.infrastructure.persistence;

import com.stablecoin.payments.merchant.iam.AbstractIntegrationTest;
import com.stablecoin.payments.merchant.iam.fixtures.IamEntityFixtures;
import com.stablecoin.payments.merchant.iam.infrastructure.persistence.repository.MerchantUserJpaRepository;
import com.stablecoin.payments.merchant.iam.infrastructure.persistence.repository.RoleJpaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("MerchantUserJpaRepository IT")
class MerchantUserJpaRepositoryIT extends AbstractIntegrationTest {

    @Autowired
    private MerchantUserJpaRepository merchantUserJpaRepository;

    @Autowired
    private RoleJpaRepository roleJpaRepository;

    @Test
    @DisplayName("should save and find user by id")
    void shouldSaveAndFindById() {
        var role = IamEntityFixtures.anAdminRole();
        roleJpaRepository.save(role);

        var user = IamEntityFixtures.anActiveUser(role);
        merchantUserJpaRepository.save(user);

        var found = merchantUserJpaRepository.findById(user.getUserId());

        assertThat(found).isPresent();
        assertThat(found.get().getFullName()).isEqualTo("Admin User");
        assertThat(found.get().getStatus()).isEqualTo("ACTIVE");
        assertThat(found.get().getEmail()).isNotEmpty();
    }

    @Test
    @DisplayName("should find user by email hash")
    void shouldFindByEmailHash() {
        var role = IamEntityFixtures.anAdminRole();
        roleJpaRepository.save(role);

        var user = IamEntityFixtures.anActiveUser(role);
        merchantUserJpaRepository.save(user);

        var found = merchantUserJpaRepository.findByEmailHash(user.getEmailHash());

        assertThat(found).isPresent();
        assertThat(found.get().getUserId()).isEqualTo(user.getUserId());
    }

    @Test
    @DisplayName("should find active users by merchant id (status not DEACTIVATED)")
    void shouldFindByStatusNot() {
        var role = IamEntityFixtures.anAdminRole();
        roleJpaRepository.save(role);

        var activeUser = IamEntityFixtures.anActiveUser(role);
        merchantUserJpaRepository.save(activeUser);

        var invitedUser = IamEntityFixtures.anInvitedUser(role);
        merchantUserJpaRepository.save(invitedUser);

        var users = merchantUserJpaRepository.findByMerchantIdAndStatusNot(
                IamEntityFixtures.defaultMerchantId(), "DEACTIVATED");

        assertThat(users).hasSize(2);
    }

    @Test
    @DisplayName("should count users by role and status")
    void shouldCountByRoleAndStatus() {
        var role = IamEntityFixtures.anAdminRole();
        roleJpaRepository.save(role);

        var user = IamEntityFixtures.anActiveUser(role);
        merchantUserJpaRepository.save(user);

        long count = merchantUserJpaRepository.countByMerchantIdAndRole_RoleNameAndStatusNot(
                IamEntityFixtures.defaultMerchantId(), "ADMIN", "DEACTIVATED");

        assertThat(count).isEqualTo(1);
    }

    @Test
    @Transactional
    @DisplayName("should deactivate all users by merchant id")
    void shouldDeactivateAll() {
        var role = roleJpaRepository.save(IamEntityFixtures.anAdminRole());

        var user1 = IamEntityFixtures.anActiveUser(role);
        var user2 = IamEntityFixtures.anInvitedUser(role);
        merchantUserJpaRepository.save(user1);
        merchantUserJpaRepository.save(user2);

        int deactivated = merchantUserJpaRepository.deactivateAllByMerchantId(
                IamEntityFixtures.defaultMerchantId(), "DEACTIVATED");

        assertThat(deactivated).isEqualTo(2);
    }

    @Test
    @DisplayName("should support optimistic locking")
    void shouldSupportOptimisticLocking() {
        var role = IamEntityFixtures.anAdminRole();
        roleJpaRepository.save(role);

        var user = IamEntityFixtures.anActiveUser(role);
        merchantUserJpaRepository.save(user);

        var saved = merchantUserJpaRepository.findById(user.getUserId()).orElseThrow();
        assertThat(saved.getVersion()).isZero();

        saved.setFullName("Updated Name");
        merchantUserJpaRepository.save(saved);

        var updated = merchantUserJpaRepository.findById(user.getUserId()).orElseThrow();
        assertThat(updated.getVersion()).isEqualTo(1L);
        assertThat(updated.getFullName()).isEqualTo("Updated Name");
    }
}
