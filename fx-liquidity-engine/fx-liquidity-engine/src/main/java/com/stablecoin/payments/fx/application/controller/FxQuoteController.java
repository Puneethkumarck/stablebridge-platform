package com.stablecoin.payments.fx.application.controller;

import com.stablecoin.payments.fx.api.request.FxQuoteRequest;
import com.stablecoin.payments.fx.api.request.FxRateLockRequest;
import com.stablecoin.payments.fx.api.response.FxQuoteResponse;
import com.stablecoin.payments.fx.api.response.FxRateLockResponse;
import com.stablecoin.payments.fx.application.mapper.FxResponseMapper;
import com.stablecoin.payments.fx.domain.service.FxQuoteCommandHandler;
import com.stablecoin.payments.fx.domain.service.FxRateLockCommandHandler;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/v1/fx")
@RequiredArgsConstructor
public class FxQuoteController {

    private final FxQuoteCommandHandler quoteCommandHandler;
    private final FxRateLockCommandHandler rateLockCommandHandler;
    private final FxResponseMapper responseMapper;

    @GetMapping("/quote")
    public FxQuoteResponse getQuote(@Valid FxQuoteRequest request) {
        log.info("GET /v1/fx/quote fromCurrency={} toCurrency={} amount={}",
                request.fromCurrency(), request.toCurrency(), request.amount());
        var quote = quoteCommandHandler.getQuote(
                request.fromCurrency(), request.toCurrency(), request.amount());
        return responseMapper.toResponse(quote);
    }

    @GetMapping("/quote/{quoteId}")
    public FxQuoteResponse getQuoteById(@PathVariable UUID quoteId) {
        log.info("GET /v1/fx/quote/{}", quoteId);
        return responseMapper.toResponse(quoteCommandHandler.getQuoteById(quoteId));
    }

    @PostMapping("/lock/{quoteId}")
    public ResponseEntity<FxRateLockResponse> lockRate(@PathVariable UUID quoteId,
                                                        @Valid @RequestBody FxRateLockRequest request) {
        log.info("POST /v1/fx/lock/{} paymentId={}", quoteId, request.paymentId());
        var result = rateLockCommandHandler.lockRate(
                quoteId, request.paymentId(), request.correlationId(),
                request.sourceCountry(), request.targetCountry());
        var status = result.created() ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(status).body(responseMapper.toResponse(result.lock()));
    }

    @DeleteMapping("/lock/{lockId}")
    public ResponseEntity<Void> releaseLock(@PathVariable UUID lockId) {
        log.info("DELETE /v1/fx/lock/{}", lockId);
        rateLockCommandHandler.releaseLock(lockId);
        return ResponseEntity.noContent().build();
    }
}
