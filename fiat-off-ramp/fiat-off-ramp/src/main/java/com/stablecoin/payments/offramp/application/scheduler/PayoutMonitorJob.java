package com.stablecoin.payments.offramp.application.scheduler;

import com.stablecoin.payments.offramp.domain.service.PayoutMonitorCommandHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.payout.monitor.enabled", havingValue = "true", matchIfMissing = true)
public class PayoutMonitorJob {

    private final PayoutMonitorCommandHandler payoutMonitorCommandHandler;

    @Scheduled(fixedDelayString = "${app.payout.monitor.interval-ms:300000}")
    public void monitorStuckPayouts() {
        log.debug("Running payout monitor — checking for stuck payouts");
        payoutMonitorCommandHandler.detectAndEscalateStuckPayouts();
    }
}
