package com.stablecoin.payments.onramp.application.controller;

import com.stablecoin.payments.onramp.domain.exception.CollectionOrderNotFoundException;
import com.stablecoin.payments.onramp.domain.exception.RefundAmountExceededException;
import com.stablecoin.payments.onramp.domain.exception.RefundNotAllowedException;
import com.stablecoin.payments.onramp.domain.exception.RefundNotFoundException;
import com.stablecoin.payments.platform.api.ApiError;
import com.stablecoin.payments.platform.infrastructure.exception.BaseGlobalExceptionHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends BaseGlobalExceptionHandler {

    @Override
    protected String errorCodePrefix() {
        return "OR";
    }

    @ResponseStatus(NOT_FOUND)
    @ExceptionHandler(CollectionOrderNotFoundException.class)
    public ApiError handleCollectionNotFound(CollectionOrderNotFoundException ex) {
        log.info("Collection order not found: {}", ex.getMessage());
        return ApiError.of(CollectionOrderNotFoundException.ERROR_CODE, NOT_FOUND.getReasonPhrase(),
                ex.getMessage());
    }

    @ResponseStatus(NOT_FOUND)
    @ExceptionHandler(RefundNotFoundException.class)
    public ApiError handleRefundNotFound(RefundNotFoundException ex) {
        log.info("Refund not found: {}", ex.getMessage());
        return ApiError.of(RefundNotFoundException.ERROR_CODE, NOT_FOUND.getReasonPhrase(),
                ex.getMessage());
    }

    @ResponseStatus(CONFLICT)
    @ExceptionHandler(RefundNotAllowedException.class)
    public ApiError handleRefundNotAllowed(RefundNotAllowedException ex) {
        log.info("Refund not allowed: {}", ex.getMessage());
        return ApiError.of(RefundNotAllowedException.ERROR_CODE, CONFLICT.getReasonPhrase(),
                ex.getMessage());
    }

    @ResponseStatus(UNPROCESSABLE_ENTITY)
    @ExceptionHandler(RefundAmountExceededException.class)
    public ApiError handleRefundAmountExceeded(RefundAmountExceededException ex) {
        log.info("Refund amount exceeded: {}", ex.getMessage());
        return ApiError.of(RefundAmountExceededException.ERROR_CODE, UNPROCESSABLE_ENTITY.getReasonPhrase(),
                ex.getMessage());
    }

    @Override
    @ResponseStatus(CONFLICT)
    @ExceptionHandler(IllegalStateException.class)
    public ApiError handleInvalidState(IllegalStateException ex) {
        log.info("Invalid state transition: {}", ex.getMessage());
        return ApiError.of("OR-0002", CONFLICT.getReasonPhrase(), ex.getMessage());
    }
}
