package com.stablecoin.payments.orchestrator.application.controller;

import jakarta.validation.constraints.NotBlank;

public record CancelPaymentRequest(
        @NotBlank(message = "reason is required")
        String reason
) {}
