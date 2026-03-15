package com.stablecoin.payments.orchestrator.domain.workflow;

import com.stablecoin.payments.orchestrator.domain.workflow.dto.CancelRequest;
import com.stablecoin.payments.orchestrator.domain.workflow.dto.ChainConfirmedSignal;
import com.stablecoin.payments.orchestrator.domain.workflow.dto.FiatCollectedSignal;
import com.stablecoin.payments.orchestrator.domain.workflow.dto.PaymentRequest;
import com.stablecoin.payments.orchestrator.domain.workflow.dto.PaymentResult;
import io.temporal.workflow.QueryMethod;
import io.temporal.workflow.SignalMethod;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

/**
 * Temporal workflow that orchestrates the cross-border payment saga.
 * <p>
 * Full sandwich flow (5 steps):
 * <ol>
 *   <li>Compliance check (S2) — read-only, no compensation</li>
 *   <li>FX rate lock (S6) — compensation: release lock</li>
 *   <li>Fiat collection (S3) — async (webhook signal), compensation: refund</li>
 *   <li>Chain transfer (S4) — async (monitor signal), compensation: return transfer</li>
 *   <li>Off-ramp payout (S5) — fire-and-forget, no compensation</li>
 * </ol>
 * <p>
 * Workflow ID convention: {@code payment_id} (natural deduplication).
 * Task queue: {@code payment-orchestrator-queue}.
 * Workflow deadline: 30 minutes.
 */
@WorkflowInterface
public interface PaymentWorkflow {

    @WorkflowMethod
    PaymentResult executePayment(PaymentRequest request);

    @SignalMethod
    void onFiatCollected(FiatCollectedSignal signal);

    @SignalMethod
    void onChainConfirmed(ChainConfirmedSignal signal);

    @SignalMethod
    void cancelPayment(CancelRequest request);

    @QueryMethod
    String getPaymentState();
}
