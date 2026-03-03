package com.stablecoin.payments.merchant.iam.application.job;

import com.stablecoin.payments.merchant.iam.infrastructure.persistence.repository.UserSessionJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Purges sessions that have expired (past {@code expires_at}) but were not explicitly revoked.
 * Runs every hour.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SessionCleanupJob {

    private final UserSessionJpaRepository sessionRepository;

    @Scheduled(fixedDelayString = "${merchant-iam.jobs.session-cleanup.fixed-delay-ms:3600000}")
    @Transactional
    public void purgeExpiredSessions() {
        var now = Instant.now();
        var deleted = sessionRepository.deleteExpiredSessions(now);
        if (deleted > 0) {
            log.info("Purged {} expired sessions at {}", deleted, now);
        } else {
            log.debug("No expired sessions to purge at {}", now);
        }
    }
}
