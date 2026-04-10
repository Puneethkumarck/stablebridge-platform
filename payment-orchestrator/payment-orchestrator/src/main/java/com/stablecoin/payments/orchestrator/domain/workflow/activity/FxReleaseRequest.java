package com.stablecoin.payments.orchestrator.domain.workflow.activity;

import java.util.UUID;

public record FxReleaseRequest(
        UUID lockId,
        UUID paymentId,
        String reason
) {}
