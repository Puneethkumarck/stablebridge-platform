package com.stablecoin.payments.orchestrator.domain.workflow;

import com.stablecoin.payments.orchestrator.domain.workflow.activity.ChainReturnRequest;
import com.stablecoin.payments.orchestrator.domain.workflow.activity.ChainTransferActivity;
import com.stablecoin.payments.orchestrator.domain.workflow.activity.ChainTransferRequest;
import com.stablecoin.payments.orchestrator.domain.workflow.activity.ChainTransferResult;
import com.stablecoin.payments.orchestrator.domain.workflow.activity.ComplianceCheckActivity;
import com.stablecoin.payments.orchestrator.domain.workflow.activity.ComplianceRequest;
import com.stablecoin.payments.orchestrator.domain.workflow.activity.ComplianceResult;
import com.stablecoin.payments.orchestrator.domain.workflow.activity.EventPublishingActivity;
import com.stablecoin.payments.orchestrator.domain.workflow.activity.FiatCollectionActivity;
import com.stablecoin.payments.orchestrator.domain.workflow.activity.FiatCollectionRequest;
import com.stablecoin.payments.orchestrator.domain.workflow.activity.FiatCollectionResult;
import com.stablecoin.payments.orchestrator.domain.workflow.activity.FiatRefundRequest;
import com.stablecoin.payments.orchestrator.domain.workflow.activity.FxLockActivity;
import com.stablecoin.payments.orchestrator.domain.workflow.activity.FxLockRequest;
import com.stablecoin.payments.orchestrator.domain.workflow.activity.FxLockResult;
import com.stablecoin.payments.orchestrator.domain.workflow.activity.FxReleaseRequest;
import com.stablecoin.payments.orchestrator.domain.workflow.activity.OffRampActivity;
import com.stablecoin.payments.orchestrator.domain.workflow.activity.OffRampRequest;
import com.stablecoin.payments.orchestrator.domain.workflow.activity.OffRampResult;
import com.stablecoin.payments.orchestrator.domain.workflow.activity.PaymentEventRequest;
import com.stablecoin.payments.orchestrator.domain.workflow.activity.PaymentStateUpdate;
import com.stablecoin.payments.orchestrator.domain.workflow.activity.UpdatePaymentStateActivity;
import com.stablecoin.payments.orchestrator.domain.workflow.dto.CancelRequest;
import com.stablecoin.payments.orchestrator.domain.workflow.dto.ChainConfirmedSignal;
import com.stablecoin.payments.orchestrator.domain.workflow.dto.FiatCollectedSignal;
import com.stablecoin.payments.orchestrator.domain.workflow.dto.PaymentRequest;
import com.stablecoin.payments.orchestrator.domain.workflow.dto.PaymentResult;
import io.temporal.activity.ActivityOptions;
import io.temporal.common.RetryOptions;
import io.temporal.workflow.Workflow;
import org.slf4j.Logger;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.UUID;

/**
 * Deterministic Temporal workflow implementation for the cross-border payment saga.
 * <p>
 * <strong>Full sandwich flow (5 steps):</strong>
 * <ol>
 *   <li>Compliance check (S2) — read-only, no compensation</li>
 *   <li>FX rate lock (S6) — compensation: release lock</li>
 *   <li>Fiat collection (S3) — async (webhook signal), compensation: refund</li>
 *   <li>Chain transfer (S4) — async (monitor signal), compensation: return transfer</li>
 *   <li>Off-ramp payout (S5) — fire-and-forget, no compensation</li>
 * </ol>
 * <p>
 * <strong>Determinism rules enforced:</strong>
 * <ul>
 *   <li>Uses {@link Workflow#currentTimeMillis()} — never {@code System.currentTimeMillis()}</li>
 *   <li>Uses {@link Workflow#getLogger(Class)} — never SLF4J directly</li>
 *   <li>No {@code Random}, no I/O, no {@code Thread.sleep}</li>
 * </ul>
 * <p>
 * <strong>Saga compensation:</strong> compensation actions are pushed onto a LIFO stack
 * after each forward step succeeds. On cancel or failure, the stack is unwound in reverse order.
 */
public class PaymentWorkflowImpl implements PaymentWorkflow {

    private static final Duration FIAT_COLLECTION_TIMEOUT = Duration.ofMinutes(30);
    private static final Duration CHAIN_CONFIRMATION_TIMEOUT = Duration.ofMinutes(30);

    private Logger log;

    private final ComplianceCheckActivity complianceActivity = Workflow.newActivityStub(
            ComplianceCheckActivity.class,
            ActivityOptions.newBuilder()
                    .setStartToCloseTimeout(Duration.ofSeconds(30))
                    .setHeartbeatTimeout(Duration.ofSeconds(10))
                    .setRetryOptions(RetryOptions.newBuilder()
                            .setMaximumAttempts(3)
                            .setInitialInterval(Duration.ofSeconds(1))
                            .setMaximumInterval(Duration.ofSeconds(5))
                            .setBackoffCoefficient(2.0)
                            .setDoNotRetry("SANCTIONS_HIT", "CORRIDOR_NOT_SUPPORTED")
                            .build())
                    .build());

    private final FxLockActivity fxLockActivity = Workflow.newActivityStub(
            FxLockActivity.class,
            ActivityOptions.newBuilder()
                    .setStartToCloseTimeout(Duration.ofSeconds(30))
                    .setRetryOptions(RetryOptions.newBuilder()
                            .setMaximumAttempts(3)
                            .setInitialInterval(Duration.ofSeconds(1))
                            .setMaximumInterval(Duration.ofSeconds(5))
                            .setBackoffCoefficient(2.0)
                            .setDoNotRetry("INSUFFICIENT_LIQUIDITY", "CORRIDOR_NOT_SUPPORTED")
                            .build())
                    .build());

    private final FiatCollectionActivity fiatCollectionActivity = Workflow.newActivityStub(
            FiatCollectionActivity.class,
            ActivityOptions.newBuilder()
                    .setStartToCloseTimeout(Duration.ofSeconds(30))
                    .setRetryOptions(RetryOptions.newBuilder()
                            .setMaximumAttempts(3)
                            .setInitialInterval(Duration.ofSeconds(1))
                            .setMaximumInterval(Duration.ofSeconds(5))
                            .setBackoffCoefficient(2.0)
                            .setDoNotRetry("PSP_REJECTED")
                            .build())
                    .build());

    private final ChainTransferActivity chainTransferActivity = Workflow.newActivityStub(
            ChainTransferActivity.class,
            ActivityOptions.newBuilder()
                    .setStartToCloseTimeout(Duration.ofSeconds(30))
                    .setRetryOptions(RetryOptions.newBuilder()
                            .setMaximumAttempts(3)
                            .setInitialInterval(Duration.ofSeconds(1))
                            .setMaximumInterval(Duration.ofSeconds(5))
                            .setBackoffCoefficient(2.0)
                            .setDoNotRetry("INSUFFICIENT_BALANCE")
                            .build())
                    .build());

    private final OffRampActivity offRampActivity = Workflow.newActivityStub(
            OffRampActivity.class,
            ActivityOptions.newBuilder()
                    .setStartToCloseTimeout(Duration.ofSeconds(30))
                    .setRetryOptions(RetryOptions.newBuilder()
                            .setMaximumAttempts(3)
                            .setInitialInterval(Duration.ofSeconds(1))
                            .setMaximumInterval(Duration.ofSeconds(5))
                            .setBackoffCoefficient(2.0)
                            .setDoNotRetry("PAYOUT_REJECTED")
                            .build())
                    .build());

    private final EventPublishingActivity eventPublishingActivity = Workflow.newActivityStub(
            EventPublishingActivity.class,
            ActivityOptions.newBuilder()
                    .setStartToCloseTimeout(Duration.ofSeconds(10))
                    .setRetryOptions(RetryOptions.newBuilder()
                            .setMaximumAttempts(3)
                            .setInitialInterval(Duration.ofSeconds(1))
                            .setMaximumInterval(Duration.ofSeconds(5))
                            .setBackoffCoefficient(2.0)
                            .build())
                    .build());

    private final UpdatePaymentStateActivity updateStateActivity = Workflow.newActivityStub(
            UpdatePaymentStateActivity.class,
            ActivityOptions.newBuilder()
                    .setStartToCloseTimeout(Duration.ofSeconds(10))
                    .setRetryOptions(RetryOptions.newBuilder()
                            .setMaximumAttempts(5)
                            .setInitialInterval(Duration.ofSeconds(1))
                            .setMaximumInterval(Duration.ofSeconds(10))
                            .setBackoffCoefficient(2.0)
                            .build())
                    .build());

    // Workflow state — deterministic, no external I/O
    private String currentState = "INITIATED";
    private boolean cancelRequested;
    private CancelRequest cancelReason;
    private final Deque<String> compensationStack = new ArrayDeque<>();

    // Async signal state
    private FiatCollectedSignal fiatCollectedSignal;
    private ChainConfirmedSignal chainConfirmedSignal;

    @Override
    public PaymentResult executePayment(PaymentRequest request) {
        log = Workflow.getLogger(PaymentWorkflowImpl.class);
        log.info("Starting payment workflow for paymentId={}", request.paymentId());

        // ── Step 1: Compliance Check ────────────────────────────────────
        currentState = "COMPLIANCE_CHECK";
        log.info("Step 1: Running compliance check for paymentId={}", request.paymentId());

        ComplianceResult complianceResult;
        try {
            complianceResult = complianceActivity.checkCompliance(new ComplianceRequest(
                    request.paymentId(),
                    request.senderId(),
                    request.recipientId(),
                    request.sourceAmount(),
                    request.sourceCurrency(),
                    request.targetCurrency(),
                    request.sourceCountry(),
                    request.targetCountry()
            ));
        } catch (Exception e) {
            currentState = "FAILED";
            var reason = "Compliance check failed: " + e.getMessage();
            log.error("Compliance check failed with exception for paymentId={}",
                    request.paymentId(), e);
            publishEvent(PaymentEventRequest.failed(request.paymentId(),
                    request.correlationId(), "COMPLIANCE_CHECK", reason, "COMPLIANCE_ERROR"));
            syncStateToDb(PaymentStateUpdate.failed(request.paymentId(), reason));
            return PaymentResult.failed(request.paymentId(), reason);
        }

        if (complianceResult.status() != ComplianceResult.ComplianceStatus.PASSED) {
            currentState = "FAILED";
            var reason = "Compliance check failed: " + complianceResult.failureReason();
            log.info("Compliance check rejected for paymentId={}: {}",
                    request.paymentId(), complianceResult.failureReason());
            publishEvent(PaymentEventRequest.failed(request.paymentId(),
                    request.correlationId(), "COMPLIANCE_CHECK", reason, "COMPLIANCE_REJECTED"));
            syncStateToDb(PaymentStateUpdate.failed(request.paymentId(), reason));
            return PaymentResult.failed(request.paymentId(), reason);
        }

        log.info("Compliance check passed for paymentId={}", request.paymentId());

        if (cancelRequested) {
            return handleCancellation(request);
        }

        // ── Step 2: FX Rate Lock ────────────────────────────────────────
        currentState = "FX_LOCKING";
        log.info("Step 2: Locking FX rate for paymentId={}", request.paymentId());

        FxLockResult fxResult;
        try {
            fxResult = fxLockActivity.lockFxRate(new FxLockRequest(
                    request.idempotencyKey(),
                    request.paymentId(),
                    request.sourceCurrency(),
                    request.targetCurrency(),
                    request.sourceAmount(),
                    request.sourceCountry(),
                    request.targetCountry()
            ));
        } catch (Exception e) {
            currentState = "FAILED";
            var reason = "FX rate lock failed: " + e.getMessage();
            log.error("FX lock failed with exception for paymentId={}",
                    request.paymentId(), e);
            publishEvent(PaymentEventRequest.failed(request.paymentId(),
                    request.correlationId(), "COMPLIANCE_CHECK", reason, "FX_LOCK_ERROR"));
            syncStateToDb(PaymentStateUpdate.failed(request.paymentId(), reason));
            return PaymentResult.failed(request.paymentId(), reason);
        }

        if (fxResult.status() != FxLockResult.FxLockStatus.LOCKED) {
            currentState = "FAILED";
            var reason = "FX rate lock failed: " + fxResult.failureReason();
            log.info("FX lock rejected for paymentId={}: {}",
                    request.paymentId(), fxResult.failureReason());
            publishEvent(PaymentEventRequest.failed(request.paymentId(),
                    request.correlationId(), "COMPLIANCE_CHECK", reason, "FX_LOCK_REJECTED"));
            syncStateToDb(PaymentStateUpdate.failed(request.paymentId(), reason));
            return PaymentResult.failed(request.paymentId(), reason);
        }

        compensationStack.push(RELEASE_FX_LOCK_PREFIX + fxResult.lockId());
        currentState = "FX_LOCKED";
        log.info("FX rate locked for paymentId={}, quoteId={}, rate={}",
                request.paymentId(), fxResult.quoteId(), fxResult.lockedRate());

        if (cancelRequested) {
            return handleCancellation(request);
        }

        // ── Step 3: Fiat Collection (S3 — async) ────────────────────────
        currentState = "FIAT_COLLECTION_PENDING";
        log.info("Step 3: Initiating fiat collection for paymentId={}", request.paymentId());

        FiatCollectionResult fiatResult;
        try {
            fiatResult = fiatCollectionActivity.initiateCollection(new FiatCollectionRequest(
                    request.paymentId(),
                    request.correlationId(),
                    request.sourceAmount(),
                    request.sourceCurrency(),
                    request.sourceCountry()
            ));
        } catch (Exception e) {
            currentState = "FAILED";
            var reason = "Fiat collection failed: " + e.getMessage();
            log.error("Fiat collection failed with exception for paymentId={}",
                    request.paymentId(), e);
            publishEvent(PaymentEventRequest.failed(request.paymentId(),
                    request.correlationId(), "FIAT_COLLECTION_PENDING", reason, "COLLECTION_ERROR"));
            return handleFailureWithCompensation(request, reason);
        }

        if (fiatResult.status() != FiatCollectionResult.FiatCollectionStatus.INITIATED) {
            var reason = "Fiat collection failed: " + fiatResult.failureReason();
            log.info("Fiat collection rejected for paymentId={}: {}",
                    request.paymentId(), fiatResult.failureReason());
            publishEvent(PaymentEventRequest.failed(request.paymentId(),
                    request.correlationId(), "FIAT_COLLECTION_PENDING", reason, "COLLECTION_REJECTED"));
            return handleFailureWithCompensation(request, reason);
        }

        compensationStack.push(REFUND_FIAT_PREFIX + fiatResult.collectionId()
                + ":" + request.sourceAmount() + ":" + request.sourceCurrency());
        log.info("Fiat collection initiated for paymentId={}, collectionId={}, waiting for signal",
                request.paymentId(), fiatResult.collectionId());

        // Wait for fiat collected signal (Stripe webhook → S3 → Kafka → S1 signal)
        boolean fiatReceived = Workflow.await(FIAT_COLLECTION_TIMEOUT,
                () -> fiatCollectedSignal != null);

        if (!fiatReceived) {
            var reason = "Fiat collection timed out after " + FIAT_COLLECTION_TIMEOUT.toMinutes() + " minutes";
            log.warn("Fiat collection timed out for paymentId={}", request.paymentId());
            publishEvent(PaymentEventRequest.failed(request.paymentId(),
                    request.correlationId(), "FIAT_COLLECTION_PENDING", reason, "COLLECTION_TIMEOUT"));
            return handleFailureWithCompensation(request, reason);
        }

        currentState = "FIAT_COLLECTED";
        log.info("Fiat collected for paymentId={}, amount={} {}",
                request.paymentId(), fiatCollectedSignal.collectedAmount(),
                fiatCollectedSignal.currency());

        if (cancelRequested) {
            return handleCancellation(request);
        }

        // ── Step 4: Chain Transfer (S4 — async) ─────────────────────────
        currentState = "ON_CHAIN_SUBMITTED";
        log.info("Step 4: Submitting chain transfer for paymentId={}", request.paymentId());

        ChainTransferResult chainResult;
        try {
            chainResult = chainTransferActivity.submitTransfer(new ChainTransferRequest(
                    request.paymentId(),
                    request.correlationId(),
                    "USDC",
                    fxResult.targetAmount(),
                    null, // S4 selects optimal wallet address
                    null  // S4 selects optimal chain
            ));
        } catch (Exception e) {
            currentState = "FAILED";
            var reason = "Chain transfer failed: " + e.getMessage();
            log.error("Chain transfer failed with exception for paymentId={}",
                    request.paymentId(), e);
            publishEvent(PaymentEventRequest.failed(request.paymentId(),
                    request.correlationId(), "ON_CHAIN_SUBMITTED", reason, "CHAIN_TRANSFER_ERROR"));
            return handleFailureWithCompensation(request, reason);
        }

        if (chainResult.status() != ChainTransferResult.ChainTransferStatus.SUBMITTED) {
            var reason = "Chain transfer failed: " + chainResult.failureReason();
            log.info("Chain transfer rejected for paymentId={}: {}",
                    request.paymentId(), chainResult.failureReason());
            publishEvent(PaymentEventRequest.failed(request.paymentId(),
                    request.correlationId(), "ON_CHAIN_SUBMITTED", reason, "CHAIN_TRANSFER_REJECTED"));
            return handleFailureWithCompensation(request, reason);
        }

        compensationStack.push(RETURN_CHAIN_PREFIX + chainResult.transferId()
                + ":" + fxResult.targetAmount() + ":USDC");
        log.info("Chain transfer submitted for paymentId={}, transferId={}, txHash={}, waiting for confirmation",
                request.paymentId(), chainResult.transferId(), chainResult.txHash());

        // Wait for chain confirmation signal (S4 monitor → Kafka → S1 signal)
        boolean chainConfirmed = Workflow.await(CHAIN_CONFIRMATION_TIMEOUT,
                () -> chainConfirmedSignal != null);

        if (!chainConfirmed) {
            var reason = "Chain confirmation timed out after " + CHAIN_CONFIRMATION_TIMEOUT.toMinutes() + " minutes";
            log.warn("Chain confirmation timed out for paymentId={}", request.paymentId());
            publishEvent(PaymentEventRequest.failed(request.paymentId(),
                    request.correlationId(), "ON_CHAIN_SUBMITTED", reason, "CHAIN_CONFIRMATION_TIMEOUT"));
            return handleFailureWithCompensation(request, reason);
        }

        currentState = "ON_CHAIN_CONFIRMED";
        log.info("Chain transfer confirmed for paymentId={}, txHash={}, block={}",
                request.paymentId(), chainConfirmedSignal.txHash(),
                chainConfirmedSignal.blockNumber());

        if (cancelRequested) {
            return handleCancellation(request);
        }

        // ── Step 5: Off-Ramp Payout (S5) ────────────────────────────────
        currentState = "OFF_RAMP_INITIATED";
        log.info("Step 5: Initiating off-ramp payout for paymentId={}", request.paymentId());

        OffRampResult offRampResult;
        try {
            offRampResult = offRampActivity.initiatePayout(new OffRampRequest(
                    request.paymentId(),
                    request.correlationId(),
                    chainResult.transferId(),
                    "USDC",
                    fxResult.targetAmount(),
                    fxResult.targetCurrency(),
                    fxResult.lockedRate(),
                    request.recipientId()
            ));
        } catch (Exception e) {
            currentState = "FAILED";
            var reason = "Off-ramp payout failed: " + e.getMessage();
            log.error("Off-ramp payout failed with exception for paymentId={}",
                    request.paymentId(), e);
            publishEvent(PaymentEventRequest.failed(request.paymentId(),
                    request.correlationId(), "OFF_RAMP_INITIATED", reason, "OFFRAMP_ERROR"));
            return handleFailureWithCompensation(request, reason);
        }

        if (offRampResult.status() != OffRampResult.OffRampStatus.INITIATED) {
            var reason = "Off-ramp payout failed: " + offRampResult.failureReason();
            log.info("Off-ramp payout rejected for paymentId={}: {}",
                    request.paymentId(), offRampResult.failureReason());
            publishEvent(PaymentEventRequest.failed(request.paymentId(),
                    request.correlationId(), "OFF_RAMP_INITIATED", reason, "OFFRAMP_REJECTED"));
            return handleFailureWithCompensation(request, reason);
        }

        // ── Settlement Complete ─────────────────────────────────────────
        currentState = "SETTLED";
        log.info("Off-ramp payout initiated for paymentId={}, payoutId={}",
                request.paymentId(), offRampResult.payoutId());

        currentState = "COMPLETED";
        log.info("Payment workflow completed for paymentId={}", request.paymentId());

        // Sync terminal state to DB
        syncStateToDb(PaymentStateUpdate.completed(
                request.paymentId(), fxResult.quoteId(), fxResult.lockedRate(),
                fxResult.targetAmount(), fxResult.targetCurrency(),
                chainResult.chainId(), chainResult.txHash()));

        return PaymentResult.completed(
                request.paymentId(),
                fxResult.quoteId(),
                fxResult.lockedRate(),
                fxResult.targetAmount(),
                fxResult.targetCurrency()
        );
    }

    @Override
    public void onFiatCollected(FiatCollectedSignal signal) {
        this.fiatCollectedSignal = signal;
    }

    @Override
    public void onChainConfirmed(ChainConfirmedSignal signal) {
        this.chainConfirmedSignal = signal;
    }

    @Override
    public void cancelPayment(CancelRequest request) {
        this.cancelRequested = true;
        this.cancelReason = request;
    }

    @Override
    public String getPaymentState() {
        return currentState;
    }

    private static final String RELEASE_FX_LOCK_PREFIX = "RELEASE_FX_LOCK:";
    private static final String REFUND_FIAT_PREFIX = "REFUND_FIAT:";
    private static final String RETURN_CHAIN_PREFIX = "RETURN_CHAIN:";

    /**
     * Runs compensation stack in LIFO order and returns a FAILED result.
     * Used when cancellation is requested between saga steps.
     */
    private PaymentResult handleCancellation(PaymentRequest request) {
        currentState = "COMPENSATING";
        var reason = cancelReason != null ? cancelReason.reason() : "Cancellation requested";
        log.info("Cancellation requested for paymentId={}, reason={}, compensationSteps={}",
                request.paymentId(), reason, compensationStack.size());

        publishEvent(PaymentEventRequest.cancelled(
                request.paymentId(), request.correlationId(), reason));

        runCompensationStack(request.paymentId(), reason);

        currentState = "FAILED";
        var failureReason = "Cancelled: " + reason;
        syncStateToDb(PaymentStateUpdate.failed(request.paymentId(), failureReason));
        return PaymentResult.failed(request.paymentId(), failureReason);
    }

    /**
     * Runs compensation stack in LIFO order and returns a FAILED result.
     * Used when a forward step fails and compensation is needed.
     */
    private PaymentResult handleFailureWithCompensation(PaymentRequest request, String reason) {
        log.info("Running compensation for paymentId={}, reason={}, compensationSteps={}",
                request.paymentId(), reason, compensationStack.size());

        runCompensationStack(request.paymentId(), reason);

        currentState = "FAILED";
        syncStateToDb(PaymentStateUpdate.failed(request.paymentId(), reason));
        return PaymentResult.failed(request.paymentId(), reason);
    }

    private void runCompensationStack(UUID paymentId, String reason) {
        while (!compensationStack.isEmpty()) {
            var step = compensationStack.pop();
            log.info("Compensating step: {} for paymentId={}", step, paymentId);
            executeCompensationStep(step, paymentId, reason);
        }
    }

    private void executeCompensationStep(String step, UUID paymentId, String reason) {
        try {
            if (step.startsWith(RELEASE_FX_LOCK_PREFIX)) {
                var lockId = UUID.fromString(step.substring(RELEASE_FX_LOCK_PREFIX.length()));
                fxLockActivity.releaseLock(new FxReleaseRequest(lockId, paymentId, reason));
                log.info("Successfully released FX lock {} for paymentId={}", lockId, paymentId);
            } else if (step.startsWith(REFUND_FIAT_PREFIX)) {
                var parts = step.substring(REFUND_FIAT_PREFIX.length()).split(":");
                var collectionId = UUID.fromString(parts[0]);
                var refundAmount = new java.math.BigDecimal(parts[1]);
                var currency = parts[2];
                fiatCollectionActivity.refundCollection(
                        new FiatRefundRequest(collectionId, paymentId, refundAmount, currency, reason));
                log.info("Successfully refunded fiat collection {} for paymentId={}",
                        collectionId, paymentId);
            } else if (step.startsWith(RETURN_CHAIN_PREFIX)) {
                var parts = step.substring(RETURN_CHAIN_PREFIX.length()).split(":");
                var transferId = UUID.fromString(parts[0]);
                var amount = new java.math.BigDecimal(parts[1]);
                var stablecoin = parts[2];
                chainTransferActivity.returnTransfer(
                        new ChainReturnRequest(transferId, paymentId, stablecoin, amount, null, reason));
                log.info("Successfully returned chain transfer {} for paymentId={}",
                        transferId, paymentId);
            } else {
                log.warn("Unknown compensation step: {} for paymentId={}", step, paymentId);
            }
        } catch (Exception e) {
            log.error("Compensation step failed: {} for paymentId={}, error={}",
                    step, paymentId, e.getMessage(), e);
        }
    }

    /**
     * Fire-and-forget event publishing. Failures are logged but do not block the saga.
     */
    private void publishEvent(PaymentEventRequest request) {
        try {
            eventPublishingActivity.publishPaymentEvent(request);
        } catch (Exception e) {
            log.warn("Event publishing failed for paymentId={}, eventType={}: {}",
                    request.paymentId(), request.eventType(), e.getMessage());
        }
    }

    /**
     * Syncs the workflow terminal state to the Payment aggregate in PostgreSQL.
     * Best-effort — failures are logged but do not block the workflow result.
     */
    private void syncStateToDb(PaymentStateUpdate update) {
        try {
            updateStateActivity.updateState(update);
        } catch (Exception e) {
            log.error("Failed to sync state to DB for paymentId={}: {}",
                    update.paymentId(), e.getMessage(), e);
        }
    }
}
