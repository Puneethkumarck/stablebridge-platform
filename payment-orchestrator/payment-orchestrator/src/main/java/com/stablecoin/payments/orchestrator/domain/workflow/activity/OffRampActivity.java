package com.stablecoin.payments.orchestrator.domain.workflow.activity;

import io.temporal.activity.ActivityInterface;

/**
 * Activity interface for initiating a fiat payout to the recipient.
 * <p>
 * Implementation calls S5 Fiat Off-Ramp via Feign to initiate a SEPA payout.
 * The payout is async (confirmed via partner webhook → Kafka event).
 * No compensation method — once stablecoin is redeemed by S5, the off-ramp
 * partner handles settlement.
 */
@ActivityInterface
public interface OffRampActivity {

    OffRampResult initiatePayout(OffRampRequest request);
}
