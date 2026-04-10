package com.stablecoin.payments.orchestrator.domain.workflow.activity;

import io.temporal.activity.ActivityInterface;

@ActivityInterface
public interface UpdatePaymentStateActivity {

    void updateState(PaymentStateUpdate update);
}
