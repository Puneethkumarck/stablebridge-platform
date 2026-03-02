package com.stablecoin.payments.merchant.onboarding.application.controller;

import com.stablecoin.payments.merchant.onboarding.domain.exceptions.InvalidMerchantStateException;
import com.stablecoin.payments.merchant.onboarding.domain.exceptions.MerchantAlreadyExistsException;
import com.stablecoin.payments.merchant.onboarding.domain.exceptions.MerchantNotFoundException;
import com.stablecoin.payments.merchant.onboarding.domain.statemachine.StateMachineException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MerchantNotFoundException.class)
    public ProblemDetail handleNotFound(MerchantNotFoundException ex) {
        log.info("Merchant not found: {}", ex.getMessage());
        return problem(HttpStatus.NOT_FOUND, ErrorCodes.MERCHANT_NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(MerchantAlreadyExistsException.class)
    public ProblemDetail handleAlreadyExists(MerchantAlreadyExistsException ex) {
        log.info("Merchant already exists: {}", ex.getMessage());
        return problem(HttpStatus.CONFLICT, ErrorCodes.MERCHANT_ALREADY_EXISTS, ex.getMessage());
    }

    @ExceptionHandler({InvalidMerchantStateException.class, StateMachineException.class})
    public ProblemDetail handleInvalidState(RuntimeException ex) {
        log.info("Invalid merchant state: {}", ex.getMessage());
        return problem(HttpStatus.UNPROCESSABLE_ENTITY, ErrorCodes.INVALID_STATE, ex.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleIllegalArgument(IllegalArgumentException ex) {
        log.info("Illegal argument: {}", ex.getMessage());
        return problem(HttpStatus.BAD_REQUEST, ErrorCodes.VALIDATION_ERROR, ex.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    public ProblemDetail handleIllegalState(IllegalStateException ex) {
        log.info("Illegal state: {}", ex.getMessage());
        return problem(HttpStatus.UNPROCESSABLE_ENTITY, ErrorCodes.INVALID_STATE, ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
        var detail = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .collect(Collectors.joining("; "));
        log.info("Validation error: {}", detail);
        return problem(HttpStatus.BAD_REQUEST, ErrorCodes.VALIDATION_ERROR, detail);
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGeneric(Exception ex) {
        log.error("Unhandled exception", ex);
        var status = HttpStatus.INTERNAL_SERVER_ERROR;
        return problem(status, ErrorCodes.INTERNAL_ERROR, status.getReasonPhrase());
    }

    private ProblemDetail problem(HttpStatus status, String typeUri, String detail) {
        var pd = ProblemDetail.forStatusAndDetail(status, detail);
        pd.setType(URI.create(typeUri));
        pd.setTitle(status.getReasonPhrase());
        return pd;
    }
}
