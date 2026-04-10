package com.stablecoin.payments.orchestrator.domain.workflow.dto;

import java.util.UUID;

public record CancelRequest(
        UUID paymentId,
        String reason,
        String cancelledBy
) {}
