package com.stablecoin.payments.merchant.iam.application.controller;

import com.stablecoin.payments.merchant.iam.domain.exceptions.BuiltInRoleModificationException;
import com.stablecoin.payments.merchant.iam.domain.exceptions.InvalidCredentialsException;
import com.stablecoin.payments.merchant.iam.domain.exceptions.InvalidUserStateException;
import com.stablecoin.payments.merchant.iam.domain.exceptions.InvitationExpiredException;
import com.stablecoin.payments.merchant.iam.domain.exceptions.InvitationNotFoundException;
import com.stablecoin.payments.merchant.iam.domain.exceptions.LastAdminException;
import com.stablecoin.payments.merchant.iam.domain.exceptions.MfaRequiredException;
import com.stablecoin.payments.merchant.iam.domain.exceptions.RoleInUseException;
import com.stablecoin.payments.merchant.iam.domain.exceptions.RoleNotFoundException;
import com.stablecoin.payments.merchant.iam.domain.exceptions.UserAlreadyExistsException;
import com.stablecoin.payments.merchant.iam.domain.exceptions.UserNotFoundException;
import com.stablecoin.payments.merchant.iam.domain.statemachine.StateMachineException;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

import static com.stablecoin.payments.merchant.iam.application.controller.ErrorCodes.BAD_REQUEST_CODE;
import static com.stablecoin.payments.merchant.iam.application.controller.ErrorCodes.BUILTIN_ROLE_MODIFY_CODE;
import static com.stablecoin.payments.merchant.iam.application.controller.ErrorCodes.INTERNAL_ERROR_CODE;
import static com.stablecoin.payments.merchant.iam.application.controller.ErrorCodes.INVALID_CREDENTIALS_CODE;
import static com.stablecoin.payments.merchant.iam.application.controller.ErrorCodes.INVALID_USER_STATE_CODE;
import static com.stablecoin.payments.merchant.iam.application.controller.ErrorCodes.INVITATION_EXPIRED_CODE;
import static com.stablecoin.payments.merchant.iam.application.controller.ErrorCodes.INVITATION_NOT_FOUND_CODE;
import static com.stablecoin.payments.merchant.iam.application.controller.ErrorCodes.LAST_ADMIN_CODE;
import static com.stablecoin.payments.merchant.iam.application.controller.ErrorCodes.MFA_REQUIRED_CODE;
import static com.stablecoin.payments.merchant.iam.application.controller.ErrorCodes.ROLE_IN_USE_CODE;
import static com.stablecoin.payments.merchant.iam.application.controller.ErrorCodes.ROLE_NOT_FOUND_CODE;
import static com.stablecoin.payments.merchant.iam.application.controller.ErrorCodes.USER_ALREADY_EXISTS_CODE;
import static com.stablecoin.payments.merchant.iam.application.controller.ErrorCodes.USER_NOT_FOUND_CODE;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.GONE;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;
import static org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ── Validation ───────────────────────────────────────────────────────────

    @ResponseStatus(BAD_REQUEST)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ApiError handleValidation(MethodArgumentNotValidException ex) {
        var errors = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.groupingBy(
                        FieldError::getField,
                        Collectors.mapping(ObjectError::getDefaultMessage, Collectors.toList())));
        log.info("Validation failed: {}", errors);
        return ApiError.withErrors(BAD_REQUEST_CODE, BAD_REQUEST.getReasonPhrase(),
                "Invalid request content", errors);
    }

    @ResponseStatus(BAD_REQUEST)
    @ExceptionHandler(ConstraintViolationException.class)
    public ApiError handleConstraintViolation(ConstraintViolationException ex) {
        var errors = ex.getConstraintViolations().stream()
                .collect(Collectors.groupingBy(
                        v -> v.getPropertyPath().toString(),
                        Collectors.mapping(v -> v.getMessage(), Collectors.toList())));
        log.info("Constraint violation: {}", errors);
        return ApiError.withErrors(BAD_REQUEST_CODE, BAD_REQUEST.getReasonPhrase(),
                "Invalid request content", errors);
    }

    // ── 401 ──────────────────────────────────────────────────────────────────

    @ResponseStatus(UNAUTHORIZED)
    @ExceptionHandler(InvalidCredentialsException.class)
    public ApiError handleInvalidCredentials(InvalidCredentialsException ex) {
        log.info("Invalid credentials: {}", ex.getMessage());
        return ApiError.of(INVALID_CREDENTIALS_CODE, UNAUTHORIZED.getReasonPhrase(), ex.getMessage());
    }

    @ResponseStatus(UNPROCESSABLE_ENTITY)
    @ExceptionHandler(MfaRequiredException.class)
    public ApiError handleMfaRequired(MfaRequiredException ex) {
        log.info("MFA required: {}", ex.getMessage());
        return ApiError.of(MFA_REQUIRED_CODE, UNPROCESSABLE_ENTITY.getReasonPhrase(), ex.getMessage());
    }

    // ── 403 ──────────────────────────────────────────────────────────────────

    @ResponseStatus(FORBIDDEN)
    @ExceptionHandler(BuiltInRoleModificationException.class)
    public ApiError handleBuiltInRoleModification(BuiltInRoleModificationException ex) {
        log.info("Built-in role modification attempt: {}", ex.getMessage());
        return ApiError.of(BUILTIN_ROLE_MODIFY_CODE, FORBIDDEN.getReasonPhrase(), ex.getMessage());
    }

    @ResponseStatus(FORBIDDEN)
    @ExceptionHandler(LastAdminException.class)
    public ApiError handleLastAdmin(LastAdminException ex) {
        log.info("Last admin violation: {}", ex.getMessage());
        return ApiError.of(LAST_ADMIN_CODE, FORBIDDEN.getReasonPhrase(), ex.getMessage());
    }

    // ── 404 ──────────────────────────────────────────────────────────────────

    @ResponseStatus(NOT_FOUND)
    @ExceptionHandler(UserNotFoundException.class)
    public ApiError handleUserNotFound(UserNotFoundException ex) {
        log.info("User not found: {}", ex.getMessage());
        return ApiError.of(USER_NOT_FOUND_CODE, NOT_FOUND.getReasonPhrase(), ex.getMessage());
    }

    @ResponseStatus(NOT_FOUND)
    @ExceptionHandler(RoleNotFoundException.class)
    public ApiError handleRoleNotFound(RoleNotFoundException ex) {
        log.info("Role not found: {}", ex.getMessage());
        return ApiError.of(ROLE_NOT_FOUND_CODE, NOT_FOUND.getReasonPhrase(), ex.getMessage());
    }

    @ResponseStatus(NOT_FOUND)
    @ExceptionHandler(InvitationNotFoundException.class)
    public ApiError handleInvitationNotFound(InvitationNotFoundException ex) {
        log.info("Invitation not found: {}", ex.getMessage());
        return ApiError.of(INVITATION_NOT_FOUND_CODE, NOT_FOUND.getReasonPhrase(), ex.getMessage());
    }

    // ── 409 / 410 ────────────────────────────────────────────────────────────

    @ResponseStatus(CONFLICT)
    @ExceptionHandler(UserAlreadyExistsException.class)
    public ApiError handleUserAlreadyExists(UserAlreadyExistsException ex) {
        log.info("User already exists: {}", ex.getMessage());
        return ApiError.of(USER_ALREADY_EXISTS_CODE, CONFLICT.getReasonPhrase(), ex.getMessage());
    }

    @ResponseStatus(CONFLICT)
    @ExceptionHandler(RoleInUseException.class)
    public ApiError handleRoleInUse(RoleInUseException ex) {
        log.info("Role in use: {}", ex.getMessage());
        return ApiError.of(ROLE_IN_USE_CODE, CONFLICT.getReasonPhrase(), ex.getMessage());
    }

    @ResponseStatus(GONE)
    @ExceptionHandler(InvitationExpiredException.class)
    public ApiError handleInvitationExpired(InvitationExpiredException ex) {
        log.info("Invitation expired: {}", ex.getMessage());
        return ApiError.of(INVITATION_EXPIRED_CODE, GONE.getReasonPhrase(), ex.getMessage());
    }

    // ── 422 ──────────────────────────────────────────────────────────────────

    @ResponseStatus(UNPROCESSABLE_ENTITY)
    @ExceptionHandler({InvalidUserStateException.class, StateMachineException.class})
    public ApiError handleInvalidState(RuntimeException ex) {
        log.info("Invalid user state: {}", ex.getMessage());
        return ApiError.of(INVALID_USER_STATE_CODE, UNPROCESSABLE_ENTITY.getReasonPhrase(), ex.getMessage());
    }

    // ── 500 ──────────────────────────────────────────────────────────────────

    @ResponseStatus(INTERNAL_SERVER_ERROR)
    @ExceptionHandler(Exception.class)
    public ApiError handleUnexpected(Exception ex) {
        log.error("Unexpected error: ", ex);
        return ApiError.of(INTERNAL_ERROR_CODE, INTERNAL_SERVER_ERROR.getReasonPhrase(),
                HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase());
    }
}
