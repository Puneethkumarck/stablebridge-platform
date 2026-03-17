package com.stablecoin.payments.compliance.application.controller;

import com.stablecoin.payments.compliance.domain.exception.CheckNotFoundException;
import com.stablecoin.payments.compliance.domain.exception.CustomerNotFoundException;
import com.stablecoin.payments.compliance.domain.exception.DuplicatePaymentException;
import com.stablecoin.payments.platform.api.ApiError;
import com.stablecoin.payments.platform.infrastructure.exception.BaseGlobalExceptionHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import static com.stablecoin.payments.compliance.application.controller.ErrorCodes.CHECK_NOT_FOUND;
import static com.stablecoin.payments.compliance.application.controller.ErrorCodes.CUSTOMER_NOT_FOUND;
import static com.stablecoin.payments.compliance.application.controller.ErrorCodes.DUPLICATE_PAYMENT;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends BaseGlobalExceptionHandler {

    @Override
    protected String errorCodePrefix() {
        return "CO";
    }

    @ResponseStatus(NOT_FOUND)
    @ExceptionHandler(CheckNotFoundException.class)
    public ApiError handleCheckNotFound(CheckNotFoundException ex) {
        log.info("Check not found: {}", ex.getMessage());
        return ApiError.of(CHECK_NOT_FOUND, NOT_FOUND.getReasonPhrase(), ex.getMessage());
    }

    @ResponseStatus(NOT_FOUND)
    @ExceptionHandler(CustomerNotFoundException.class)
    public ApiError handleCustomerNotFound(CustomerNotFoundException ex) {
        log.info("Customer not found: {}", ex.getMessage());
        return ApiError.of(CUSTOMER_NOT_FOUND, NOT_FOUND.getReasonPhrase(), ex.getMessage());
    }

    @ResponseStatus(CONFLICT)
    @ExceptionHandler(DuplicatePaymentException.class)
    public ApiError handleDuplicatePayment(DuplicatePaymentException ex) {
        log.info("Duplicate payment: {}", ex.getMessage());
        return ApiError.of(DUPLICATE_PAYMENT, CONFLICT.getReasonPhrase(), ex.getMessage());
    }
}
