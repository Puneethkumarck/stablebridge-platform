package com.stablecoin.payments.orchestrator.application.controller;

import com.stablecoin.payments.orchestrator.domain.service.PaymentCommandHandler;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentCommandHandler commandHandler;

    @PostMapping
    public ResponseEntity<PaymentResponse> initiatePayment(
            @NotBlank(message = "Idempotency-Key header is required")
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody InitiatePaymentRequest request) {
        log.info("POST /v1/payments idempotencyKey={}, senderId={}", idempotencyKey, request.senderId());

        var result = commandHandler.initiatePayment(
                idempotencyKey,
                UUID.randomUUID(),
                request.senderId(),
                request.recipientId(),
                request.sourceAmount(),
                request.sourceCurrency(),
                request.targetCurrency(),
                request.sourceCountry(),
                request.targetCountry()
        );

        var response = PaymentResponse.from(result.payment());
        var status = result.replay() ? HttpStatus.OK : HttpStatus.CREATED;

        return ResponseEntity.status(status).body(response);
    }

    @GetMapping("/{paymentId}")
    public PaymentResponse getPayment(@PathVariable UUID paymentId) {
        log.info("GET /v1/payments/{}", paymentId);
        return PaymentResponse.from(commandHandler.getPayment(paymentId));
    }

    @PostMapping("/{paymentId}/cancel")
    public PaymentResponse cancelPayment(
            @PathVariable UUID paymentId,
            @Valid @RequestBody CancelPaymentRequest request) {
        log.info("POST /v1/payments/{}/cancel reason={}", paymentId, request.reason());
        return PaymentResponse.from(commandHandler.cancelPayment(paymentId, request.reason()));
    }
}
