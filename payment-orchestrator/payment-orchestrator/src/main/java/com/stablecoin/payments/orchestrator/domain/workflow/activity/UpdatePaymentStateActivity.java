package com.stablecoin.payments.orchestrator.domain.workflow.activity;

import io.temporal.activity.ActivityInterface;

/**
 * Activity interface for syncing the Temporal workflow's terminal state
 * back to the Payment aggregate in PostgreSQL.
 * <p>
 * Called at the end of every workflow execution path (COMPLETED or FAILED)
 * to ensure the database reflects the workflow result. Temporal retries
 * on transient DB failures, guaranteeing eventual consistency.
 */
@ActivityInterface
public interface UpdatePaymentStateActivity {

    void updateState(PaymentStateUpdate update);
}
