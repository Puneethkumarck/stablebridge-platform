package com.stablecoin.payments.orchestrator.domain.workflow.activity;

import io.temporal.activity.ActivityInterface;

/**
 * Activity interface for submitting a stablecoin transfer on-chain.
 * <p>
 * Implementation calls S4 Blockchain &amp; Custody via Feign to submit a USDC
 * transfer. The transfer is async (confirmed via block monitor → Kafka signal).
 * Includes a compensation method to return transferred stablecoin.
 */
@ActivityInterface
public interface ChainTransferActivity {

    ChainTransferResult submitTransfer(ChainTransferRequest request);

    void returnTransfer(ChainReturnRequest request);
}
