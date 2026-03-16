package com.stablecoin.payments.gateway.iam.application.job;

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
        int totalDeleted = 0;
        int deleted;
        do {
            deleted = jdbcTemplate.update(
                    "WITH expired AS ("
                    + "SELECT idempotency_key, request_method, request_path "
                    + "FROM gatewayiam_idempotency_keys WHERE expires_at < NOW() LIMIT 1000"
                    + ") DELETE FROM gatewayiam_idempotency_keys USING expired "
                    + "WHERE gatewayiam_idempotency_keys.idempotency_key = expired.idempotency_key "
                    + "AND gatewayiam_idempotency_keys.request_method = expired.request_method "
                    + "AND gatewayiam_idempotency_keys.request_path = expired.request_path");
            totalDeleted += deleted;
        } while (deleted >= 1000);
        if (totalDeleted > 0) {
            log.info("Cleaned up {} expired idempotency keys", totalDeleted);
        }
    }
}
