package com.stablecoin.payments.offramp.application.controller;

import com.stablecoin.payments.offramp.domain.exception.PayoutNotFoundException;
import com.stablecoin.payments.offramp.domain.exception.PayoutNotRefundableException;
import com.stablecoin.payments.offramp.domain.exception.PayoutPartnerException;
import com.stablecoin.payments.offramp.domain.exception.RedemptionFailedException;
import com.stablecoin.payments.platform.api.ApiError;
import com.stablecoin.payments.platform.infrastructure.exception.BaseGlobalExceptionHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import static org.springframework.http.HttpStatus.BAD_GATEWAY;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends BaseGlobalExceptionHandler {

    @Override
    protected String errorCodePrefix() {
        return "OF";
    }

    @ResponseStatus(NOT_FOUND)
    @ExceptionHandler(PayoutNotFoundException.class)
    public ApiError handlePayoutNotFound(PayoutNotFoundException ex) {
        log.info("Payout not found: {}", ex.getMessage());
        return ApiError.of(PayoutNotFoundException.ERROR_CODE, NOT_FOUND.getReasonPhrase(),
                ex.getMessage());
    }

    @ResponseStatus(UNPROCESSABLE_ENTITY)
    @ExceptionHandler(RedemptionFailedException.class)
    public ApiError handleRedemptionFailed(RedemptionFailedException ex) {
        log.warn("Redemption failed: {}", ex.getMessage());
        return ApiError.of(RedemptionFailedException.ERROR_CODE, UNPROCESSABLE_ENTITY.getReasonPhrase(),
                ex.getMessage());
    }

    @ResponseStatus(BAD_GATEWAY)
    @ExceptionHandler(PayoutPartnerException.class)
    public ApiError handlePayoutPartnerError(PayoutPartnerException ex) {
        log.warn("Payout partner error: {}", ex.getMessage());
        return ApiError.of(PayoutPartnerException.ERROR_CODE, BAD_GATEWAY.getReasonPhrase(),
                ex.getMessage());
    }

    @ResponseStatus(CONFLICT)
    @ExceptionHandler(PayoutNotRefundableException.class)
    public ApiError handlePayoutNotRefundable(PayoutNotRefundableException ex) {
        log.info("Payout not refundable: {}", ex.getMessage());
        return ApiError.of(PayoutNotRefundableException.ERROR_CODE, CONFLICT.getReasonPhrase(),
                ex.getMessage());
    }

    @Override
    @ResponseStatus(CONFLICT)
    @ExceptionHandler(IllegalStateException.class)
    public ApiError handleInvalidState(IllegalStateException ex) {
        log.info("Invalid state transition: {}", ex.getMessage());
        return ApiError.of("OF-0002", CONFLICT.getReasonPhrase(), ex.getMessage());
    }
}
