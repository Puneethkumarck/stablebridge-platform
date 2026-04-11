package com.stablecoin.payments.orchestrator.domain.workflow.activity;

import io.temporal.activity.ActivityInterface;

@ActivityInterface
public interface FxLockActivity {

    FxLockResult lockFxRate(FxLockRequest request);

    void releaseLock(FxReleaseRequest request);
}
