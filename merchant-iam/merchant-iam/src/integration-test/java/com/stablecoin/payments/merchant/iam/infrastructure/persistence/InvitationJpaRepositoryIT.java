package com.stablecoin.payments.merchant.iam.infrastructure.persistence;

import com.stablecoin.payments.merchant.iam.AbstractIntegrationTest;
import com.stablecoin.payments.merchant.iam.fixtures.IamEntityFixtures;
import com.stablecoin.payments.merchant.iam.infrastructure.persistence.repository.InvitationJpaRepository;
import com.stablecoin.payments.merchant.iam.infrastructure.persistence.repository.RoleJpaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("InvitationJpaRepository IT")
class InvitationJpaRepositoryIT extends AbstractIntegrationTest {

    @Autowired
    private InvitationJpaRepository invitationJpaRepository;

    @Autowired
    private RoleJpaRepository roleJpaRepository;

    @Test
    @DisplayName("should find invitation by token hash")
    void shouldFindByTokenHash() {
        var role = IamEntityFixtures.anAdminRole();
        roleJpaRepository.save(role);

        var invitation = IamEntityFixtures.aPendingInvitation(role);
        invitationJpaRepository.save(invitation);

        var found = invitationJpaRepository.findByTokenHash(invitation.getTokenHash());

        assertThat(found).isPresent();
        assertThat(found.get().getStatus()).isEqualTo("PENDING");
        assertThat(found.get().getMerchantId()).isEqualTo(IamEntityFixtures.defaultMerchantId());
    }

    @Test
    @DisplayName("should find pending invitations by merchant id")
    void shouldFindByMerchantIdAndStatus() {
        var role = IamEntityFixtures.anAdminRole();
        roleJpaRepository.save(role);

        var pending = IamEntityFixtures.aPendingInvitation(role);
        var expired = IamEntityFixtures.anExpiredInvitation(role);
        expired.setStatus("EXPIRED");
        invitationJpaRepository.save(pending);
        invitationJpaRepository.save(expired);

        var pendingInvitations = invitationJpaRepository.findByMerchantIdAndStatus(
                IamEntityFixtures.defaultMerchantId(), "PENDING");

        assertThat(pendingInvitations).hasSize(1);
        assertThat(pendingInvitations.getFirst().getTokenHash()).isEqualTo(pending.getTokenHash());
    }

    @Test
    @Transactional
    @DisplayName("should expire overdue invitations")
    void shouldExpireOverdueInvitations() {
        var role = IamEntityFixtures.anAdminRole();
        roleJpaRepository.save(role);

        var expiredInvitation = IamEntityFixtures.anExpiredInvitation(role);
        invitationJpaRepository.save(expiredInvitation);

        var validInvitation = IamEntityFixtures.aPendingInvitation(role);
        invitationJpaRepository.save(validInvitation);

        int expired = invitationJpaRepository.expireOverdueInvitations(Instant.now());

        assertThat(expired).isEqualTo(1);
    }
}
