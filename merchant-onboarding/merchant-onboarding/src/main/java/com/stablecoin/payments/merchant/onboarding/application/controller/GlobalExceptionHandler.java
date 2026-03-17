package com.stablecoin.payments.merchant.onboarding.application.controller;

import com.stablecoin.payments.merchant.onboarding.domain.exceptions.InvalidMerchantStateException;
import com.stablecoin.payments.merchant.onboarding.domain.exceptions.MerchantAlreadyExistsException;
import com.stablecoin.payments.merchant.onboarding.domain.exceptions.MerchantNotFoundException;
import com.stablecoin.payments.merchant.onboarding.domain.statemachine.StateMachineException;
import com.stablecoin.payments.platform.api.ApiError;
import com.stablecoin.payments.platform.infrastructure.exception.BaseGlobalExceptionHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import static com.stablecoin.payments.merchant.onboarding.application.controller.ErrorCodes.INTERNAL_ERROR_CODE;
import static com.stablecoin.payments.merchant.onboarding.application.controller.ErrorCodes.INVALID_STATE_CODE;
import static com.stablecoin.payments.merchant.onboarding.application.controller.ErrorCodes.MERCHANT_ALREADY_EXISTS_CODE;
import static com.stablecoin.payments.merchant.onboarding.application.controller.ErrorCodes.MERCHANT_NOT_FOUND_CODE;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends BaseGlobalExceptionHandler {

    @Override
    protected String errorCodePrefix() {
        return "MO";
    }

    @ResponseStatus(NOT_FOUND)
    @ExceptionHandler(MerchantNotFoundException.class)
    public ApiError handleNotFound(MerchantNotFoundException ex) {
        log.info("Merchant not found: {}", ex.getMessage());
        return ApiError.of(MERCHANT_NOT_FOUND_CODE, NOT_FOUND.getReasonPhrase(), ex.getMessage());
    }

    @ResponseStatus(CONFLICT)
    @ExceptionHandler(MerchantAlreadyExistsException.class)
    public ApiError handleAlreadyExists(MerchantAlreadyExistsException ex) {
        log.info("Merchant already exists: {}", ex.getMessage());
        return ApiError.of(MERCHANT_ALREADY_EXISTS_CODE, CONFLICT.getReasonPhrase(), ex.getMessage());
    }

    @ResponseStatus(UNPROCESSABLE_ENTITY)
    @ExceptionHandler({InvalidMerchantStateException.class, StateMachineException.class})
    public ApiError handleInvalidMerchantState(RuntimeException ex) {
        log.info("Invalid merchant state: {}", ex.getMessage());
        return ApiError.of(INVALID_STATE_CODE, UNPROCESSABLE_ENTITY.getReasonPhrase(), ex.getMessage());
    }

    @Override
    @ResponseStatus(INTERNAL_SERVER_ERROR)
    @ExceptionHandler(Exception.class)
    public ApiError handleUnexpected(Exception ex) {
        log.error("Unexpected error: ", ex);
        return ApiError.of(INTERNAL_ERROR_CODE, INTERNAL_SERVER_ERROR.getReasonPhrase(),
                HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase());
    }
}
