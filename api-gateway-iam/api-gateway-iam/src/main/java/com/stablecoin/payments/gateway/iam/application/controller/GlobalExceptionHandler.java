package com.stablecoin.payments.gateway.iam.application.controller;

import com.stablecoin.payments.gateway.iam.domain.exception.ApiKeyExpiredException;
import com.stablecoin.payments.gateway.iam.domain.exception.ApiKeyNotFoundException;
import com.stablecoin.payments.gateway.iam.domain.exception.ApiKeyRevokedException;
import com.stablecoin.payments.gateway.iam.domain.exception.InvalidClientCredentialsException;
import com.stablecoin.payments.gateway.iam.domain.exception.IpNotAllowedException;
import com.stablecoin.payments.gateway.iam.domain.exception.MerchantAccessDeniedException;
import com.stablecoin.payments.gateway.iam.domain.exception.MerchantNotActiveException;
import com.stablecoin.payments.gateway.iam.domain.exception.MerchantNotFoundException;
import com.stablecoin.payments.gateway.iam.domain.exception.OAuthClientNotFoundException;
import com.stablecoin.payments.gateway.iam.domain.exception.RateLimitExceededException;
import com.stablecoin.payments.gateway.iam.domain.exception.ScopeExceededException;
import com.stablecoin.payments.gateway.iam.domain.exception.TokenRevokedException;
import com.stablecoin.payments.platform.api.ApiError;
import com.stablecoin.payments.platform.infrastructure.exception.BaseGlobalExceptionHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.TOO_MANY_REQUESTS;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends BaseGlobalExceptionHandler {

    @Override
    protected String errorCodePrefix() {
        return "GW";
    }

    @ResponseStatus(FORBIDDEN)
    @ExceptionHandler(MerchantAccessDeniedException.class)
    public ApiError handleMerchantAccessDenied(MerchantAccessDeniedException ex) {
        log.info("Merchant access denied: {}", ex.getMessage());
        return ApiError.of("GW-2003", FORBIDDEN.getReasonPhrase(), ex.getMessage());
    }

    @ResponseStatus(UNAUTHORIZED)
    @ExceptionHandler(InvalidClientCredentialsException.class)
    public ApiError handleInvalidCredentials(InvalidClientCredentialsException ex) {
        log.info("Invalid client credentials: {}", ex.getMessage());
        return ApiError.of("GW-1001", UNAUTHORIZED.getReasonPhrase(), ex.getMessage());
    }

    @ResponseStatus(NOT_FOUND)
    @ExceptionHandler(MerchantNotFoundException.class)
    public ApiError handleMerchantNotFound(MerchantNotFoundException ex) {
        return ApiError.of("GW-2001", NOT_FOUND.getReasonPhrase(), ex.getMessage());
    }

    @ResponseStatus(FORBIDDEN)
    @ExceptionHandler(MerchantNotActiveException.class)
    public ApiError handleMerchantNotActive(MerchantNotActiveException ex) {
        return ApiError.of("GW-2002", FORBIDDEN.getReasonPhrase(), ex.getMessage());
    }

    @ResponseStatus(NOT_FOUND)
    @ExceptionHandler(ApiKeyNotFoundException.class)
    public ApiError handleApiKeyNotFound(ApiKeyNotFoundException ex) {
        return ApiError.of("GW-3001", NOT_FOUND.getReasonPhrase(), ex.getMessage());
    }

    @ResponseStatus(FORBIDDEN)
    @ExceptionHandler(ApiKeyRevokedException.class)
    public ApiError handleApiKeyRevoked(ApiKeyRevokedException ex) {
        return ApiError.of("GW-3002", FORBIDDEN.getReasonPhrase(), ex.getMessage());
    }

    @ResponseStatus(FORBIDDEN)
    @ExceptionHandler(ApiKeyExpiredException.class)
    public ApiError handleApiKeyExpired(ApiKeyExpiredException ex) {
        return ApiError.of("GW-3003", FORBIDDEN.getReasonPhrase(), ex.getMessage());
    }

    @ResponseStatus(FORBIDDEN)
    @ExceptionHandler(IpNotAllowedException.class)
    public ApiError handleIpNotAllowed(IpNotAllowedException ex) {
        return ApiError.of("GW-3004", FORBIDDEN.getReasonPhrase(), ex.getMessage());
    }

    @ResponseStatus(FORBIDDEN)
    @ExceptionHandler(ScopeExceededException.class)
    public ApiError handleScopeExceeded(ScopeExceededException ex) {
        return ApiError.of("GW-3005", FORBIDDEN.getReasonPhrase(), ex.getMessage());
    }

    @ResponseStatus(BAD_REQUEST)
    @ExceptionHandler(TokenRevokedException.class)
    public ApiError handleTokenRevoked(TokenRevokedException ex) {
        return ApiError.of("GW-4001", BAD_REQUEST.getReasonPhrase(), ex.getMessage());
    }

    @ResponseStatus(NOT_FOUND)
    @ExceptionHandler(OAuthClientNotFoundException.class)
    public ApiError handleOAuthClientNotFound(OAuthClientNotFoundException ex) {
        return ApiError.of("GW-5001", NOT_FOUND.getReasonPhrase(), ex.getMessage());
    }

    @ResponseStatus(TOO_MANY_REQUESTS)
    @ExceptionHandler(RateLimitExceededException.class)
    public ApiError handleRateLimitExceeded(RateLimitExceededException ex) {
        return ApiError.of("GW-6001", TOO_MANY_REQUESTS.getReasonPhrase(), ex.getMessage());
    }
}
