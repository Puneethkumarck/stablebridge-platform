package com.stablecoin.payments.orchestrator.domain.workflow.activity;

import io.temporal.activity.ActivityInterface;

@ActivityInterface
public interface FiatCollectionActivity {

    FiatCollectionResult initiateCollection(FiatCollectionRequest request);

    void refundCollection(FiatRefundRequest request);
}
