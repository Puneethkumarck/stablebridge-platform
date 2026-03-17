package com.stablecoin.payments.custody.application.controller;

import com.stablecoin.payments.custody.domain.exception.ChainUnavailableException;
import com.stablecoin.payments.custody.domain.exception.CustodySigningException;
import com.stablecoin.payments.custody.domain.exception.InsufficientBalanceException;
import com.stablecoin.payments.custody.domain.exception.TransferNotFoundException;
import com.stablecoin.payments.custody.domain.exception.WalletNotFoundException;
import com.stablecoin.payments.platform.api.ApiError;
import com.stablecoin.payments.platform.infrastructure.exception.BaseGlobalExceptionHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE;
import static org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends BaseGlobalExceptionHandler {

    @Override
    protected String errorCodePrefix() {
        return "BC";
    }

    @ResponseStatus(UNPROCESSABLE_ENTITY)
    @ExceptionHandler(InsufficientBalanceException.class)
    public ApiError handleInsufficientBalance(InsufficientBalanceException ex) {
        log.info("Insufficient balance: {}", ex.getMessage());
        return ApiError.of(InsufficientBalanceException.ERROR_CODE,
                UNPROCESSABLE_ENTITY.getReasonPhrase(), ex.getMessage());
    }

    @ResponseStatus(SERVICE_UNAVAILABLE)
    @ExceptionHandler(ChainUnavailableException.class)
    public ApiError handleChainUnavailable(ChainUnavailableException ex) {
        log.warn("Chain unavailable: {}", ex.getMessage());
        return ApiError.of(ChainUnavailableException.ERROR_CODE,
                SERVICE_UNAVAILABLE.getReasonPhrase(), ex.getMessage());
    }

    @ResponseStatus(NOT_FOUND)
    @ExceptionHandler(TransferNotFoundException.class)
    public ApiError handleTransferNotFound(TransferNotFoundException ex) {
        log.info("Transfer not found: {}", ex.getMessage());
        return ApiError.of(TransferNotFoundException.ERROR_CODE,
                NOT_FOUND.getReasonPhrase(), ex.getMessage());
    }

    @ResponseStatus(NOT_FOUND)
    @ExceptionHandler(WalletNotFoundException.class)
    public ApiError handleWalletNotFound(WalletNotFoundException ex) {
        log.info("Wallet not found: {}", ex.getMessage());
        return ApiError.of(WalletNotFoundException.ERROR_CODE,
                NOT_FOUND.getReasonPhrase(), ex.getMessage());
    }

    @ResponseStatus(INTERNAL_SERVER_ERROR)
    @ExceptionHandler(CustodySigningException.class)
    public ApiError handleCustodySigning(CustodySigningException ex) {
        log.error("Custody signing error: {}", ex.getClass().getSimpleName());
        return ApiError.of(CustodySigningException.ERROR_CODE,
                INTERNAL_SERVER_ERROR.getReasonPhrase(), "Custody signing failed");
    }

    @Override
    @ResponseStatus(CONFLICT)
    @ExceptionHandler(IllegalStateException.class)
    public ApiError handleInvalidState(IllegalStateException ex) {
        log.info("Invalid state transition: {}", ex.getMessage());
        return ApiError.of("BC-0002", CONFLICT.getReasonPhrase(), ex.getMessage());
    }
}
