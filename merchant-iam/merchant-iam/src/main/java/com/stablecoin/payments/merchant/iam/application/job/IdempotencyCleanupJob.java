package com.stablecoin.payments.merchant.iam.application.job;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnProperty(name = "app.idempotency.cleanup.enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class IdempotencyCleanupJob {

    private final JdbcTemplate jdbcTemplate;

    @Scheduled(cron = "${app.idempotency.cleanup-cron:0 0 * * * *}")
    public void cleanExpiredKeys() {
        var deleted = jdbcTemplate.update(
                "DELETE FROM merchantiam_idempotency_keys WHERE expires_at < NOW()");
        if (deleted > 0) {
            log.info("Cleaned up {} expired idempotency keys", deleted);
        }
    }
}
