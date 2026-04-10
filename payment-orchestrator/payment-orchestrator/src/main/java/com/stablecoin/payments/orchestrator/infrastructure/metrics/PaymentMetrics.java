package com.stablecoin.payments.orchestrator.infrastructure.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PaymentMetrics {

    private final MeterRegistry meterRegistry;

    public void recordPaymentInitiated(String corridor, String currency) {
        meterRegistry.counter("payment.initiated",
                "corridor", corridor,
                "currency", currency
        ).increment();
    }

    public void recordPaymentCompleted(String corridor, String currency) {
        meterRegistry.counter("payment.completed",
                "corridor", corridor,
                "currency", currency
        ).increment();
    }

    public void recordPaymentFailed(String corridor, String reason) {
        meterRegistry.counter("payment.failed",
                "corridor", corridor,
                "reason", reason
        ).increment();
    }

    public Timer.Sample startPaymentTimer() {
        return Timer.start(meterRegistry);
    }

    public void recordPaymentDuration(Timer.Sample sample, String corridor) {
        sample.stop(meterRegistry.timer("payment.duration", "corridor", corridor));
    }
}
