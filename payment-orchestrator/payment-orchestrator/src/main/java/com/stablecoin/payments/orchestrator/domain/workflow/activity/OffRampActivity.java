package com.stablecoin.payments.orchestrator.domain.workflow.activity;

import io.temporal.activity.ActivityInterface;

@ActivityInterface
public interface OffRampActivity {

    OffRampResult initiatePayout(OffRampRequest request);
}
