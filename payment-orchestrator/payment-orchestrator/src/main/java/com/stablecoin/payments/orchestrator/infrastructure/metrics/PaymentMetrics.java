package com.stablecoin.payments.orchestrator.infrastructure.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Custom business metrics for the Payment Orchestrator service.
 *
 * <p>Tracks payment initiation, completion, failure counts and lifecycle duration.
 */
@Component
@RequiredArgsConstructor
public class PaymentMetrics {

    private final MeterRegistry meterRegistry;

    /**
     * Records a payment initiation event.
     */
    public void recordPaymentInitiated(String corridor, String currency) {
        meterRegistry.counter("payment.initiated",
                "corridor", corridor,
                "currency", currency
        ).increment();
    }

    /**
     * Records a payment completion event.
     */
    public void recordPaymentCompleted(String corridor, String currency) {
        meterRegistry.counter("payment.completed",
                "corridor", corridor,
                "currency", currency
        ).increment();
    }

    /**
     * Records a payment failure event.
     */
    public void recordPaymentFailed(String corridor, String reason) {
        meterRegistry.counter("payment.failed",
                "corridor", corridor,
                "reason", reason
        ).increment();
    }

    /**
     * Starts a timer sample for measuring payment lifecycle duration.
     */
    public Timer.Sample startPaymentTimer() {
        return Timer.start(meterRegistry);
    }

    /**
     * Stops the timer sample and records the payment lifecycle duration.
     */
    public void recordPaymentDuration(Timer.Sample sample, String corridor) {
        sample.stop(meterRegistry.timer("payment.duration", "corridor", corridor));
    }
}
