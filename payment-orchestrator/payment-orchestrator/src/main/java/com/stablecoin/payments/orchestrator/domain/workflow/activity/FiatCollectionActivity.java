package com.stablecoin.payments.orchestrator.domain.workflow.activity;

import io.temporal.activity.ActivityInterface;

/**
 * Activity interface for initiating fiat collection from the sender's bank.
 * <p>
 * Implementation calls S3 Fiat On-Ramp via Feign to create a collection order.
 * The collection is async (confirmed via Stripe webhook → Kafka signal).
 * Includes a compensation method to refund a completed collection.
 */
@ActivityInterface
public interface FiatCollectionActivity {

    FiatCollectionResult initiateCollection(FiatCollectionRequest request);

    void refundCollection(FiatRefundRequest request);
}
