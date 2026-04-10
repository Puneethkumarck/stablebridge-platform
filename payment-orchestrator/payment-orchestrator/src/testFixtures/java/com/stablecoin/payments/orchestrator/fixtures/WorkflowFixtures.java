package com.stablecoin.payments.orchestrator.fixtures;

import com.stablecoin.payments.orchestrator.domain.workflow.activity.ChainTransferResult;
import com.stablecoin.payments.orchestrator.domain.workflow.activity.FiatCollectionResult;
import com.stablecoin.payments.orchestrator.domain.workflow.activity.FxLockResult;
import com.stablecoin.payments.orchestrator.domain.workflow.activity.OffRampResult;
import com.stablecoin.payments.orchestrator.domain.workflow.dto.ChainConfirmedSignal;
import com.stablecoin.payments.orchestrator.domain.workflow.dto.FiatCollectedSignal;
import com.stablecoin.payments.orchestrator.domain.workflow.dto.PaymentRequest;

import java.math.BigDecimal;
import java.util.UUID;

import static com.stablecoin.payments.orchestrator.domain.workflow.activity.ChainTransferResult.ChainTransferStatus.SUBMITTED;
import static com.stablecoin.payments.orchestrator.domain.workflow.activity.FiatCollectionResult.FiatCollectionStatus.INITIATED;
import static com.stablecoin.payments.orchestrator.domain.workflow.activity.FxLockResult.FxLockStatus.LOCKED;
import static com.stablecoin.payments.orchestrator.domain.workflow.activity.OffRampResult.OffRampStatus;
import static com.stablecoin.payments.orchestrator.fixtures.PaymentFixtures.IDEMPOTENCY_KEY;
import static com.stablecoin.payments.orchestrator.fixtures.PaymentFixtures.RECIPIENT_ID;
import static com.stablecoin.payments.orchestrator.fixtures.PaymentFixtures.SENDER_ID;

public final class WorkflowFixtures {

    public static final UUID PAYMENT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    public static final UUID QUOTE_ID = UUID.fromString("00000000-0000-0000-0000-000000000099");
    public static final UUID LOCK_ID = UUID.fromString("00000000-0000-0000-0000-000000000077");
    public static final UUID CHECK_ID = UUID.fromString("00000000-0000-0000-0000-000000000088");
    public static final UUID COLLECTION_ID = UUID.fromString("00000000-0000-0000-0000-000000000055");
    public static final UUID TRANSFER_ID = UUID.fromString("00000000-0000-0000-0000-000000000066");
    public static final UUID PAYOUT_ID = UUID.fromString("00000000-0000-0000-0000-000000000044");
    public static final String TX_HASH = "0xabc123def456";
    public static final String CHAIN_ID = "base";

    private WorkflowFixtures() {}

    public static PaymentRequest aPaymentRequest() {
        return new PaymentRequest(
                PAYMENT_ID,
                IDEMPOTENCY_KEY,
                UUID.fromString("00000000-0000-0000-0000-000000000010"),
                SENDER_ID,
                RECIPIENT_ID,
                new BigDecimal("1000.00"),
                "USD",
                "EUR",
                "US",
                "DE"
        );
    }

    public static FxLockResult aLockedFxResult() {
        return new FxLockResult(
                LOCK_ID, QUOTE_ID, new BigDecimal("0.92"),
                new BigDecimal("920.00"), "EUR",
                LOCKED, null);
    }

    public static FiatCollectionResult anInitiatedCollectionResult() {
        return new FiatCollectionResult(COLLECTION_ID, INITIATED, null);
    }

    public static FiatCollectedSignal aFiatCollectedSignal() {
        return new FiatCollectedSignal(
                PAYMENT_ID, "pi_stripe_123",
                new BigDecimal("1000.00"), "USD");
    }

    public static ChainTransferResult aSubmittedTransferResult() {
        return new ChainTransferResult(
                TRANSFER_ID, CHAIN_ID, TX_HASH, SUBMITTED, null);
    }

    public static ChainConfirmedSignal aChainConfirmedSignal() {
        return new ChainConfirmedSignal(PAYMENT_ID, TX_HASH, CHAIN_ID, 12345L);
    }

    public static OffRampResult anInitiatedPayoutResult() {
        return new OffRampResult(PAYOUT_ID, OffRampStatus.INITIATED, null);
    }
}
