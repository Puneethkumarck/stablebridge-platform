package com.stablecoin.payments.merchant.iam.application.job;

import com.stablecoin.payments.merchant.iam.infrastructure.persistence.repository.InvitationJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Marks PENDING invitations whose {@code expires_at} has passed as EXPIRED.
 * Runs every 15 minutes.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InvitationExpiryJob {

    private final InvitationJpaRepository invitationRepository;

    @Scheduled(fixedDelayString = "${merchant-iam.jobs.invitation-expiry.fixed-delay-ms:900000}")
    @Transactional
    public void expireOverdueInvitations() {
        var now = Instant.now();
        var expired = invitationRepository.expireOverdueInvitations(now);
        if (expired > 0) {
            log.info("Expired {} overdue invitations at {}", expired, now);
        } else {
            log.debug("No overdue invitations found at {}", now);
        }
    }
}
