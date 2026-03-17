package com.stablecoin.payments.ledger.application.controller;

import com.stablecoin.payments.ledger.domain.exception.AccountNotFoundException;
import com.stablecoin.payments.ledger.domain.exception.DuplicateTransactionException;
import com.stablecoin.payments.ledger.domain.exception.JournalNotFoundException;
import com.stablecoin.payments.ledger.domain.exception.ReconciliationNotFoundException;
import com.stablecoin.payments.platform.api.ApiError;
import com.stablecoin.payments.platform.infrastructure.exception.BaseGlobalExceptionHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends BaseGlobalExceptionHandler {

    @Override
    protected String errorCodePrefix() {
        return "LD";
    }

    @ResponseStatus(NOT_FOUND)
    @ExceptionHandler(JournalNotFoundException.class)
    public ApiError handleJournalNotFound(JournalNotFoundException ex) {
        log.info("Journal not found: {}", ex.getMessage());
        return ApiError.of(ex.errorCode(), NOT_FOUND.getReasonPhrase(), ex.getMessage());
    }

    @ResponseStatus(NOT_FOUND)
    @ExceptionHandler(AccountNotFoundException.class)
    public ApiError handleAccountNotFound(AccountNotFoundException ex) {
        log.info("Account not found: {}", ex.getMessage());
        return ApiError.of(ex.errorCode(), NOT_FOUND.getReasonPhrase(), ex.getMessage());
    }

    @ResponseStatus(NOT_FOUND)
    @ExceptionHandler(ReconciliationNotFoundException.class)
    public ApiError handleReconciliationNotFound(ReconciliationNotFoundException ex) {
        log.info("Reconciliation not found: {}", ex.getMessage());
        return ApiError.of(ex.errorCode(), NOT_FOUND.getReasonPhrase(), ex.getMessage());
    }

    @ResponseStatus(CONFLICT)
    @ExceptionHandler(DuplicateTransactionException.class)
    public ApiError handleDuplicateTransaction(DuplicateTransactionException ex) {
        log.info("Duplicate transaction: {}", ex.getMessage());
        return ApiError.of(ex.errorCode(), CONFLICT.getReasonPhrase(), ex.getMessage());
    }
}
