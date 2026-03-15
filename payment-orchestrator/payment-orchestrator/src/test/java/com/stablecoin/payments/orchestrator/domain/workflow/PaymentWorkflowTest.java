package com.stablecoin.payments.orchestrator.domain.workflow;

import com.stablecoin.payments.orchestrator.domain.workflow.activity.ChainReturnRequest;
import com.stablecoin.payments.orchestrator.domain.workflow.activity.ChainTransferActivity;
import com.stablecoin.payments.orchestrator.domain.workflow.activity.ChainTransferResult;
import com.stablecoin.payments.orchestrator.domain.workflow.activity.ComplianceCheckActivity;
import com.stablecoin.payments.orchestrator.domain.workflow.activity.ComplianceResult;
import com.stablecoin.payments.orchestrator.domain.workflow.activity.EventPublishingActivity;
import com.stablecoin.payments.orchestrator.domain.workflow.activity.FiatCollectionActivity;
import com.stablecoin.payments.orchestrator.domain.workflow.activity.FiatCollectionResult;
import com.stablecoin.payments.orchestrator.domain.workflow.activity.FiatRefundRequest;
import com.stablecoin.payments.orchestrator.domain.workflow.activity.FxLockActivity;
import com.stablecoin.payments.orchestrator.domain.workflow.activity.FxReleaseRequest;
import com.stablecoin.payments.orchestrator.domain.workflow.activity.OffRampActivity;
import com.stablecoin.payments.orchestrator.domain.workflow.activity.OffRampResult;
import com.stablecoin.payments.orchestrator.domain.workflow.activity.PaymentEventRequest;
import com.stablecoin.payments.orchestrator.domain.workflow.activity.UpdatePaymentStateActivity;
import com.stablecoin.payments.orchestrator.domain.workflow.dto.CancelRequest;
import com.stablecoin.payments.orchestrator.domain.workflow.dto.PaymentResult;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.testing.TestWorkflowExtension;
import io.temporal.worker.Worker;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.math.BigDecimal;

import static com.stablecoin.payments.orchestrator.domain.workflow.activity.ChainTransferResult.ChainTransferStatus.FAILED;
import static com.stablecoin.payments.orchestrator.domain.workflow.activity.ComplianceResult.ComplianceStatus.PASSED;
import static com.stablecoin.payments.orchestrator.domain.workflow.activity.ComplianceResult.ComplianceStatus.SANCTIONS_HIT;
import static com.stablecoin.payments.orchestrator.domain.workflow.activity.FiatCollectionResult.FiatCollectionStatus;
import static com.stablecoin.payments.orchestrator.domain.workflow.activity.FxLockResult.FxLockStatus.INSUFFICIENT_LIQUIDITY;
import static com.stablecoin.payments.orchestrator.domain.workflow.activity.OffRampResult.OffRampStatus;
import static com.stablecoin.payments.orchestrator.fixtures.WorkflowFixtures.CHECK_ID;
import static com.stablecoin.payments.orchestrator.fixtures.WorkflowFixtures.COLLECTION_ID;
import static com.stablecoin.payments.orchestrator.fixtures.WorkflowFixtures.LOCK_ID;
import static com.stablecoin.payments.orchestrator.fixtures.WorkflowFixtures.PAYMENT_ID;
import static com.stablecoin.payments.orchestrator.fixtures.WorkflowFixtures.QUOTE_ID;
import static com.stablecoin.payments.orchestrator.fixtures.WorkflowFixtures.TRANSFER_ID;
import static com.stablecoin.payments.orchestrator.fixtures.WorkflowFixtures.aChainConfirmedSignal;
import static com.stablecoin.payments.orchestrator.fixtures.WorkflowFixtures.aFiatCollectedSignal;
import static com.stablecoin.payments.orchestrator.fixtures.WorkflowFixtures.aLockedFxResult;
import static com.stablecoin.payments.orchestrator.fixtures.WorkflowFixtures.aPaymentRequest;
import static com.stablecoin.payments.orchestrator.fixtures.WorkflowFixtures.aSubmittedTransferResult;
import static com.stablecoin.payments.orchestrator.fixtures.WorkflowFixtures.anInitiatedCollectionResult;
import static com.stablecoin.payments.orchestrator.fixtures.WorkflowFixtures.anInitiatedPayoutResult;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

@DisplayName("PaymentWorkflow")
class PaymentWorkflowTest {

    private final ComplianceCheckActivity complianceActivity = mock(ComplianceCheckActivity.class);
    private final FxLockActivity fxLockActivity = mock(FxLockActivity.class);
    private final FiatCollectionActivity fiatCollectionActivity = mock(FiatCollectionActivity.class);
    private final ChainTransferActivity chainTransferActivity = mock(ChainTransferActivity.class);
    private final OffRampActivity offRampActivity = mock(OffRampActivity.class);
    private final UpdatePaymentStateActivity updateStateActivity = mock(UpdatePaymentStateActivity.class);
    private final EventPublishingActivity eventPublishingActivity = mock(EventPublishingActivity.class);

    @RegisterExtension
    public TestWorkflowExtension testWorkflow = TestWorkflowExtension.newBuilder()
            .setWorkflowTypes(PaymentWorkflowImpl.class)
            .setActivityImplementations(
                    complianceActivity, fxLockActivity, fiatCollectionActivity,
                    chainTransferActivity, offRampActivity, updateStateActivity,
                    eventPublishingActivity)
            .build();

    @Nested
    @DisplayName("full sandwich happy path")
    class FullSandwichHappyPath {

        @Test
        @DisplayName("should complete full payment sandwich: compliance → FX → fiat → chain → off-ramp")
        void shouldCompleteFullSandwich(WorkflowClient workflowClient, Worker worker) {
            given(complianceActivity.checkCompliance(any()))
                    .willReturn(new ComplianceResult(CHECK_ID, PASSED, null));
            given(fxLockActivity.lockFxRate(any()))
                    .willReturn(aLockedFxResult());

            // Fiat collection: return INITIATED, send signal synchronously from activity
            given(fiatCollectionActivity.initiateCollection(any()))
                    .willAnswer(invocation -> {
                        var stub = workflowClient.newWorkflowStub(
                                PaymentWorkflow.class, "payment-" + PAYMENT_ID);
                        stub.onFiatCollected(aFiatCollectedSignal());
                        return anInitiatedCollectionResult();
                    });

            // Chain transfer: return SUBMITTED, send confirmation signal synchronously
            given(chainTransferActivity.submitTransfer(any()))
                    .willAnswer(invocation -> {
                        var stub = workflowClient.newWorkflowStub(
                                PaymentWorkflow.class, "payment-" + PAYMENT_ID);
                        stub.onChainConfirmed(aChainConfirmedSignal());
                        return aSubmittedTransferResult();
                    });

            given(offRampActivity.initiatePayout(any()))
                    .willReturn(anInitiatedPayoutResult());

            startWorkflow(workflowClient, worker);
            var result = getResult(workflowClient);

            var expected = PaymentResult.completed(
                    PAYMENT_ID, QUOTE_ID,
                    new BigDecimal("0.92"), new BigDecimal("920.00"), "EUR");

            assertThat(result)
                    .usingRecursiveComparison()
                    .isEqualTo(expected);

            then(complianceActivity).should().checkCompliance(any());
            then(fxLockActivity).should().lockFxRate(any());
            then(fiatCollectionActivity).should().initiateCollection(any());
            then(chainTransferActivity).should().submitTransfer(any());
            then(offRampActivity).should().initiatePayout(any());
        }
    }

    @Nested
    @DisplayName("compliance failure")
    class ComplianceFailure {

        @Test
        @DisplayName("should return FAILED when compliance check fails")
        void shouldFailWhenComplianceCheckFails(WorkflowClient workflowClient,
                                                Worker worker) {
            given(complianceActivity.checkCompliance(any()))
                    .willReturn(new ComplianceResult(CHECK_ID,
                            ComplianceResult.ComplianceStatus.FAILED, "PEP match"));

            startWorkflow(workflowClient, worker);
            var result = getResult(workflowClient);

            var expected = PaymentResult.failed(PAYMENT_ID, "Compliance check failed: PEP match");
            assertThat(result)
                    .usingRecursiveComparison()
                    .isEqualTo(expected);

            then(fxLockActivity).should(never()).lockFxRate(any());
            then(fiatCollectionActivity).should(never()).initiateCollection(any());

            var expectedEvent = PaymentEventRequest.failed(
                    PAYMENT_ID, aPaymentRequest().correlationId(),
                    "COMPLIANCE_CHECK", "Compliance check failed: PEP match",
                    "COMPLIANCE_REJECTED");
            then(eventPublishingActivity).should().publishPaymentEvent(expectedEvent);
        }

        @Test
        @DisplayName("should return FAILED when compliance detects sanctions hit")
        void shouldFailOnSanctionsHit(WorkflowClient workflowClient,
                                      Worker worker) {
            given(complianceActivity.checkCompliance(any()))
                    .willReturn(new ComplianceResult(CHECK_ID, SANCTIONS_HIT,
                            "OFAC sanctions list match"));

            startWorkflow(workflowClient, worker);
            var result = getResult(workflowClient);

            var expected = PaymentResult.failed(PAYMENT_ID,
                    "Compliance check failed: OFAC sanctions list match");
            assertThat(result)
                    .usingRecursiveComparison()
                    .isEqualTo(expected);

            then(fxLockActivity).should(never()).lockFxRate(any());
            then(fiatCollectionActivity).should(never()).initiateCollection(any());

            var expectedEvent = PaymentEventRequest.failed(
                    PAYMENT_ID, aPaymentRequest().correlationId(),
                    "COMPLIANCE_CHECK", "Compliance check failed: OFAC sanctions list match",
                    "COMPLIANCE_REJECTED");
            then(eventPublishingActivity).should().publishPaymentEvent(expectedEvent);
        }
    }

    @Nested
    @DisplayName("FX lock failure")
    class FxLockFailure {

        @Test
        @DisplayName("should return FAILED when FX rate lock fails — no compensation needed")
        void shouldFailWhenFxLockFails(WorkflowClient workflowClient,
                                       Worker worker) {
            given(complianceActivity.checkCompliance(any()))
                    .willReturn(new ComplianceResult(CHECK_ID, PASSED, null));
            given(fxLockActivity.lockFxRate(any()))
                    .willReturn(new com.stablecoin.payments.orchestrator.domain.workflow.activity.FxLockResult(
                            null, null, null, null, null,
                            INSUFFICIENT_LIQUIDITY, "No liquidity for USD/EUR"));

            startWorkflow(workflowClient, worker);
            var result = getResult(workflowClient);

            var expected = PaymentResult.failed(PAYMENT_ID,
                    "FX rate lock failed: No liquidity for USD/EUR");
            assertThat(result)
                    .usingRecursiveComparison()
                    .isEqualTo(expected);

            then(fiatCollectionActivity).should(never()).initiateCollection(any());

            var expectedEvent = PaymentEventRequest.failed(
                    PAYMENT_ID, aPaymentRequest().correlationId(),
                    "FX_LOCKING", "FX rate lock failed: No liquidity for USD/EUR",
                    "FX_LOCK_REJECTED");
            then(eventPublishingActivity).should().publishPaymentEvent(expectedEvent);
        }
    }

    @Nested
    @DisplayName("fiat collection failure")
    class FiatCollectionFailure {

        @Test
        @DisplayName("should fail and release FX lock when fiat collection is rejected")
        void shouldFailAndReleaseFxLockWhenCollectionRejected(WorkflowClient workflowClient,
                                                               Worker worker) {
            given(complianceActivity.checkCompliance(any()))
                    .willReturn(new ComplianceResult(CHECK_ID, PASSED, null));
            given(fxLockActivity.lockFxRate(any()))
                    .willReturn(aLockedFxResult());
            given(fiatCollectionActivity.initiateCollection(any()))
                    .willReturn(new FiatCollectionResult(null,
                            FiatCollectionStatus.FAILED, "PSP declined"));

            startWorkflow(workflowClient, worker);
            var result = getResult(workflowClient);

            var expected = PaymentResult.failed(PAYMENT_ID,
                    "Fiat collection failed: PSP declined");
            assertThat(result)
                    .usingRecursiveComparison()
                    .isEqualTo(expected);

            // Should release FX lock as compensation
            then(fxLockActivity).should().releaseLock(
                    new FxReleaseRequest(LOCK_ID, PAYMENT_ID,
                            "Fiat collection failed: PSP declined"));
            then(chainTransferActivity).should(never()).submitTransfer(any());
        }
    }

    @Nested
    @DisplayName("chain transfer failure")
    class ChainTransferFailure {

        @Test
        @DisplayName("should fail and compensate fiat + FX when chain transfer is rejected")
        void shouldCompensateFiatAndFxWhenChainTransferFails(WorkflowClient workflowClient,
                                                              Worker worker) {
            given(complianceActivity.checkCompliance(any()))
                    .willReturn(new ComplianceResult(CHECK_ID, PASSED, null));
            given(fxLockActivity.lockFxRate(any()))
                    .willReturn(aLockedFxResult());

            given(fiatCollectionActivity.initiateCollection(any()))
                    .willAnswer(invocation -> {
                        var stub = workflowClient.newWorkflowStub(
                                PaymentWorkflow.class, "payment-" + PAYMENT_ID);
                        stub.onFiatCollected(aFiatCollectedSignal());
                        return anInitiatedCollectionResult();
                    });

            // Chain transfer fails
            given(chainTransferActivity.submitTransfer(any()))
                    .willReturn(new ChainTransferResult(null, null, null,
                            FAILED, "Insufficient balance"));

            startWorkflow(workflowClient, worker);
            var result = getResult(workflowClient);

            var expected = PaymentResult.failed(PAYMENT_ID,
                    "Chain transfer failed: Insufficient balance");
            assertThat(result)
                    .usingRecursiveComparison()
                    .isEqualTo(expected);

            // LIFO compensation: refund fiat first, then release FX lock
            then(fiatCollectionActivity).should().refundCollection(
                    new FiatRefundRequest(COLLECTION_ID, PAYMENT_ID,
                            new BigDecimal("1000.00"), "USD",
                            "Chain transfer failed: Insufficient balance"));
            then(fxLockActivity).should().releaseLock(
                    new FxReleaseRequest(LOCK_ID, PAYMENT_ID,
                            "Chain transfer failed: Insufficient balance"));
            then(offRampActivity).should(never()).initiatePayout(any());
        }
    }

    @Nested
    @DisplayName("off-ramp failure")
    class OffRampFailure {

        @Test
        @DisplayName("should fail and compensate chain + fiat + FX when off-ramp is rejected")
        void shouldCompensateAllWhenOffRampFails(WorkflowClient workflowClient,
                                                  Worker worker) {
            given(complianceActivity.checkCompliance(any()))
                    .willReturn(new ComplianceResult(CHECK_ID, PASSED, null));
            given(fxLockActivity.lockFxRate(any()))
                    .willReturn(aLockedFxResult());

            given(fiatCollectionActivity.initiateCollection(any()))
                    .willAnswer(invocation -> {
                        var stub = workflowClient.newWorkflowStub(
                                PaymentWorkflow.class, "payment-" + PAYMENT_ID);
                        stub.onFiatCollected(aFiatCollectedSignal());
                        return anInitiatedCollectionResult();
                    });

            given(chainTransferActivity.submitTransfer(any()))
                    .willAnswer(invocation -> {
                        var stub = workflowClient.newWorkflowStub(
                                PaymentWorkflow.class, "payment-" + PAYMENT_ID);
                        stub.onChainConfirmed(aChainConfirmedSignal());
                        return aSubmittedTransferResult();
                    });

            // Off-ramp fails
            given(offRampActivity.initiatePayout(any()))
                    .willReturn(new OffRampResult(null, OffRampStatus.FAILED,
                            "Payout partner rejected"));

            startWorkflow(workflowClient, worker);
            var result = getResult(workflowClient);

            var expected = PaymentResult.failed(PAYMENT_ID,
                    "Off-ramp payout failed: Payout partner rejected");
            assertThat(result)
                    .usingRecursiveComparison()
                    .isEqualTo(expected);

            // LIFO compensation: return chain, refund fiat, release FX lock
            then(chainTransferActivity).should().returnTransfer(
                    new ChainReturnRequest(TRANSFER_ID, PAYMENT_ID,
                            "USDC", new BigDecimal("920.00"), null,
                            "Off-ramp payout failed: Payout partner rejected"));
            then(fiatCollectionActivity).should().refundCollection(
                    new FiatRefundRequest(COLLECTION_ID, PAYMENT_ID,
                            new BigDecimal("1000.00"), "USD",
                            "Off-ramp payout failed: Payout partner rejected"));
            then(fxLockActivity).should().releaseLock(
                    new FxReleaseRequest(LOCK_ID, PAYMENT_ID,
                            "Off-ramp payout failed: Payout partner rejected"));
        }
    }

    @Nested
    @DisplayName("cancel signal")
    class CancelSignal {

        @Test
        @DisplayName("should release FX lock when cancel received after FX lock succeeds")
        void shouldReleaseFxLockOnCancelAfterFxLock(WorkflowClient workflowClient,
                                                     Worker worker) {
            given(complianceActivity.checkCompliance(any()))
                    .willReturn(new ComplianceResult(CHECK_ID, PASSED, null));

            given(fxLockActivity.lockFxRate(any()))
                    .willAnswer(invocation -> {
                        var stub = workflowClient.newWorkflowStub(
                                PaymentWorkflow.class, "payment-" + PAYMENT_ID);
                        stub.cancelPayment(new CancelRequest(
                                PAYMENT_ID, "Customer requested cancellation", "customer"));
                        return aLockedFxResult();
                    });

            startWorkflow(workflowClient, worker);
            var result = getResult(workflowClient);

            var expected = PaymentResult.failed(PAYMENT_ID,
                    "Cancelled: Customer requested cancellation");
            assertThat(result)
                    .usingRecursiveComparison()
                    .isEqualTo(expected);

            then(fxLockActivity).should().releaseLock(new FxReleaseRequest(
                    LOCK_ID, PAYMENT_ID, "Customer requested cancellation"));
            then(fiatCollectionActivity).should(never()).initiateCollection(any());

            var expectedEvent = PaymentEventRequest.cancelled(
                    PAYMENT_ID, aPaymentRequest().correlationId(),
                    "Customer requested cancellation");
            then(eventPublishingActivity).should().publishPaymentEvent(expectedEvent);
        }

        @Test
        @DisplayName("should not call releaseLock when cancel before FX lock")
        void shouldNotReleaseLockWhenCancelBeforeFxLock(WorkflowClient workflowClient,
                                                        Worker worker) {
            given(complianceActivity.checkCompliance(any()))
                    .willAnswer(invocation -> {
                        var stub = workflowClient.newWorkflowStub(
                                PaymentWorkflow.class, "payment-" + PAYMENT_ID);
                        stub.cancelPayment(new CancelRequest(
                                PAYMENT_ID, "Changed mind", "customer"));
                        return new ComplianceResult(CHECK_ID, PASSED, null);
                    });

            startWorkflow(workflowClient, worker);
            var result = getResult(workflowClient);

            var expected = PaymentResult.failed(PAYMENT_ID, "Cancelled: Changed mind");
            assertThat(result)
                    .usingRecursiveComparison()
                    .isEqualTo(expected);

            then(fxLockActivity).should(never()).lockFxRate(any());
            then(fxLockActivity).should(never()).releaseLock(any());
            then(fiatCollectionActivity).should(never()).initiateCollection(any());

            var expectedEvent = PaymentEventRequest.cancelled(
                    PAYMENT_ID, aPaymentRequest().correlationId(), "Changed mind");
            then(eventPublishingActivity).should().publishPaymentEvent(expectedEvent);
        }

        @Test
        @DisplayName("should return FAILED even when compensation activity throws")
        void shouldReturnFailedWhenCompensationThrows(WorkflowClient workflowClient,
                                                       Worker worker) {
            given(complianceActivity.checkCompliance(any()))
                    .willReturn(new ComplianceResult(CHECK_ID, PASSED, null));

            given(fxLockActivity.lockFxRate(any()))
                    .willAnswer(invocation -> {
                        var stub = workflowClient.newWorkflowStub(
                                PaymentWorkflow.class, "payment-" + PAYMENT_ID);
                        stub.cancelPayment(new CancelRequest(
                                PAYMENT_ID, "Timeout", "system"));
                        return aLockedFxResult();
                    });

            willThrow(new RuntimeException("FX engine unavailable"))
                    .given(fxLockActivity).releaseLock(any());

            startWorkflow(workflowClient, worker);
            var result = getResult(workflowClient);

            var expected = PaymentResult.failed(PAYMENT_ID, "Cancelled: Timeout");
            assertThat(result)
                    .usingRecursiveComparison()
                    .isEqualTo(expected);

            then(fxLockActivity).should(atLeast(1)).releaseLock(new FxReleaseRequest(
                    LOCK_ID, PAYMENT_ID, "Timeout"));
        }
    }

    @Nested
    @DisplayName("query method")
    class QueryMethod {

        @Test
        @DisplayName("should return current state via query after completion")
        void shouldReturnCurrentStateViaQuery(WorkflowClient workflowClient,
                                               Worker worker) {
            given(complianceActivity.checkCompliance(any()))
                    .willReturn(new ComplianceResult(CHECK_ID, PASSED, null));
            given(fxLockActivity.lockFxRate(any()))
                    .willReturn(aLockedFxResult());

            given(fiatCollectionActivity.initiateCollection(any()))
                    .willAnswer(invocation -> {
                        var stub = workflowClient.newWorkflowStub(
                                PaymentWorkflow.class, "payment-" + PAYMENT_ID);
                        stub.onFiatCollected(aFiatCollectedSignal());
                        return anInitiatedCollectionResult();
                    });

            given(chainTransferActivity.submitTransfer(any()))
                    .willAnswer(invocation -> {
                        var stub = workflowClient.newWorkflowStub(
                                PaymentWorkflow.class, "payment-" + PAYMENT_ID);
                        stub.onChainConfirmed(aChainConfirmedSignal());
                        return aSubmittedTransferResult();
                    });

            given(offRampActivity.initiatePayout(any()))
                    .willReturn(anInitiatedPayoutResult());

            startWorkflow(workflowClient, worker);
            var result = getResult(workflowClient);

            var expected = PaymentResult.completed(
                    PAYMENT_ID, QUOTE_ID,
                    new BigDecimal("0.92"), new BigDecimal("920.00"), "EUR");
            assertThat(result)
                    .usingRecursiveComparison()
                    .isEqualTo(expected);
        }
    }

    private PaymentWorkflow startWorkflow(WorkflowClient client, Worker worker) {
        var options = WorkflowOptions.newBuilder()
                .setWorkflowId("payment-" + PAYMENT_ID)
                .setTaskQueue(worker.getTaskQueue())
                .build();
        var workflow = client.newWorkflowStub(PaymentWorkflow.class, options);
        WorkflowClient.start(workflow::executePayment, aPaymentRequest());
        return workflow;
    }

    private PaymentResult getResult(WorkflowClient client) {
        return client.newUntypedWorkflowStub("payment-" + PAYMENT_ID)
                .getResult(PaymentResult.class);
    }
}
