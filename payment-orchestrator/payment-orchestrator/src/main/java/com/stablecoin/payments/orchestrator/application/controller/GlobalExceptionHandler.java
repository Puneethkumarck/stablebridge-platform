package com.stablecoin.payments.orchestrator.application.controller;

import com.stablecoin.payments.orchestrator.domain.model.PaymentNotCancellableException;
import com.stablecoin.payments.orchestrator.domain.model.PaymentNotFoundException;
import com.stablecoin.payments.platform.api.ApiError;
import com.stablecoin.payments.platform.infrastructure.exception.BaseGlobalExceptionHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.FieldError;
import org.springframework.validation.method.ParameterValidationResult;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.stablecoin.payments.orchestrator.application.controller.ErrorCodes.PAYMENT_NOT_CANCELLABLE;
import static com.stablecoin.payments.orchestrator.application.controller.ErrorCodes.PAYMENT_NOT_FOUND;
import static com.stablecoin.payments.orchestrator.application.controller.ErrorCodes.VALIDATION_ERROR;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends BaseGlobalExceptionHandler {

    @Override
    protected String errorCodePrefix() {
        return "PO";
    }

    @ResponseStatus(BAD_REQUEST)
    @ExceptionHandler(HandlerMethodValidationException.class)
    public ApiError handleMethodValidation(HandlerMethodValidationException ex) {
        Map<String, List<String>> errors = ex.getParameterValidationResults().stream()
                .flatMap(result -> result.getResolvableErrors().stream()
                        .map(error -> Map.entry(
                                resolveParameterName(result, error),
                                error.getDefaultMessage())))
                .collect(Collectors.groupingBy(
                        Map.Entry::getKey,
                        Collectors.mapping(Map.Entry::getValue, Collectors.toList())));
        log.info("Method validation failed: {}", errors);
        return ApiError.withErrors(VALIDATION_ERROR, BAD_REQUEST.getReasonPhrase(),
                "Invalid request content", errors);
    }

    private static String resolveParameterName(ParameterValidationResult result,
                                                org.springframework.context.MessageSourceResolvable error) {
        if (error instanceof FieldError fieldError) {
            return fieldError.getField();
        }
        var paramName = result.getMethodParameter().getParameterName();
        return paramName != null ? paramName : "unknown";
    }

    @ResponseStatus(NOT_FOUND)
    @ExceptionHandler(PaymentNotFoundException.class)
    public ApiError handlePaymentNotFound(PaymentNotFoundException ex) {
        log.info("Payment not found: {}", ex.getMessage());
        return ApiError.of(PAYMENT_NOT_FOUND, NOT_FOUND.getReasonPhrase(), ex.getMessage());
    }

    @ResponseStatus(CONFLICT)
    @ExceptionHandler(PaymentNotCancellableException.class)
    public ApiError handlePaymentNotCancellable(PaymentNotCancellableException ex) {
        log.info("Payment not cancellable: {}", ex.getMessage());
        return ApiError.of(PAYMENT_NOT_CANCELLABLE, CONFLICT.getReasonPhrase(), ex.getMessage());
    }
}
