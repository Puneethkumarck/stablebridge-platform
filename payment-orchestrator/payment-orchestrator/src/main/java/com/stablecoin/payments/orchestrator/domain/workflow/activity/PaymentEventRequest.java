package com.stablecoin.payments.orchestrator.domain.workflow.activity;

import com.stablecoin.payments.orchestrator.domain.event.PaymentCompensationStarted;
import com.stablecoin.payments.orchestrator.domain.event.PaymentFailed;

import java.util.UUID;

public record PaymentEventRequest(
        String eventType,
        UUID paymentId,
        UUID correlationId,
        String failedState,
        String reason,
        String errorCode
) {

    public static PaymentEventRequest failed(UUID paymentId, UUID correlationId,
                                             String failedState, String reason,
                                             String errorCode) {
        return new PaymentEventRequest(PaymentFailed.TOPIC, paymentId, correlationId,
                failedState, reason, errorCode);
    }

    public static PaymentEventRequest cancelled(UUID paymentId, UUID correlationId,
                                                String reason) {
        return new PaymentEventRequest(PaymentCompensationStarted.TOPIC, paymentId, correlationId,
                null, reason, null);
    }
}
