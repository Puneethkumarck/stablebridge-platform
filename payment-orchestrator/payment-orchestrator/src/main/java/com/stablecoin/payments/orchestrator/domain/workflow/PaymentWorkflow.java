package com.stablecoin.payments.orchestrator.domain.workflow;

import com.stablecoin.payments.orchestrator.domain.workflow.dto.CancelRequest;
import com.stablecoin.payments.orchestrator.domain.workflow.dto.ChainConfirmedSignal;
import com.stablecoin.payments.orchestrator.domain.workflow.dto.FiatCollectedSignal;
import com.stablecoin.payments.orchestrator.domain.workflow.dto.PaymentRequest;
import com.stablecoin.payments.orchestrator.domain.workflow.dto.PaymentResult;
import io.temporal.workflow.QueryMethod;
import io.temporal.workflow.SignalMethod;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

@WorkflowInterface
public interface PaymentWorkflow {

    @WorkflowMethod
    PaymentResult executePayment(PaymentRequest request);

    @SignalMethod
    void onFiatCollected(FiatCollectedSignal signal);

    @SignalMethod
    void onChainConfirmed(ChainConfirmedSignal signal);

    @SignalMethod
    void cancelPayment(CancelRequest request);

    @QueryMethod
    String getPaymentState();
}
