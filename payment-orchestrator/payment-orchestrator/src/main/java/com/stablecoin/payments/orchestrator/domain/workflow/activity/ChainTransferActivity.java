package com.stablecoin.payments.orchestrator.domain.workflow.activity;

import io.temporal.activity.ActivityInterface;

@ActivityInterface
public interface ChainTransferActivity {

    ChainTransferResult submitTransfer(ChainTransferRequest request);

    void returnTransfer(ChainReturnRequest request);
}
