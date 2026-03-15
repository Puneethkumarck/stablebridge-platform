package com.stablecoin.payments.orchestrator.infrastructure.activity;

import com.stablecoin.payments.orchestrator.domain.model.ChainId;
import com.stablecoin.payments.orchestrator.domain.model.FxRate;
import com.stablecoin.payments.orchestrator.domain.model.Money;
import com.stablecoin.payments.orchestrator.domain.port.PaymentRepository;
import com.stablecoin.payments.orchestrator.domain.workflow.activity.PaymentStateUpdate;
import com.stablecoin.payments.orchestrator.domain.workflow.activity.UpdatePaymentStateActivity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Temporal activity that syncs the workflow terminal state back to the
 * Payment aggregate in PostgreSQL.
 * <p>
 * Called at the end of every workflow execution path. Uses the domain
 * aggregate's state machine methods to ensure valid transitions.
 * <p>
 * Idempotent: if the payment is already in a terminal state, the update
 * is silently skipped (workflow replay or retry scenario).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UpdatePaymentStateActivityImpl implements UpdatePaymentStateActivity {

    private final PaymentRepository paymentRepository;

    @Override
    @Transactional
    public void updateState(PaymentStateUpdate update) {
        log.info("Updating payment state paymentId={} to {}", update.paymentId(), update.terminalState());

        var payment = paymentRepository.findById(update.paymentId())
                .orElseThrow(() -> new IllegalStateException(
                        "Payment not found: " + update.paymentId()));

        // Idempotent: skip if already terminal (workflow replay or retry)
        if (payment.isTerminal()) {
            log.info("Payment {} already in terminal state {}, skipping update",
                    update.paymentId(), payment.state());
            return;
        }

        var updated = switch (update.terminalState()) {
            case "COMPLETED" -> {
                var now = Instant.now();
                var fxRate = update.lockedRate() != null
                        ? new FxRate(update.quoteId(), "USD", update.targetCurrency(),
                                update.lockedRate(), now, now.plusSeconds(600), "workflow")
                        : null;
                var targetAmount = update.targetAmount() != null
                        ? new Money(update.targetAmount(), update.targetCurrency())
                        : null;
                var chainId = update.chainId() != null ? new ChainId(update.chainId()) : null;
                yield payment.completeFromWorkflow(fxRate, targetAmount, chainId, update.txHash());
            }
            case "FAILED" -> payment.fail(update.failureReason());
            default -> throw new IllegalArgumentException(
                    "Unsupported terminal state: " + update.terminalState());
        };

        paymentRepository.save(updated);
        log.info("Payment {} state updated to {}", update.paymentId(), updated.state());
    }
}
