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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

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
import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    @Test
    void should_return_unauthorized_for_invalid_credentials() {
        var ex = InvalidCredentialsException.invalidEmailOrPassword();

        var result = handler.handleInvalidCredentials(ex);

        assertThat(result).extracting("code", "status")
                .containsExactly(INVALID_CREDENTIALS_CODE, "Unauthorized");
    }

    @Test
    void should_return_unprocessable_for_mfa_required() {
        var ex = MfaRequiredException.forUser(UUID.randomUUID(), UUID.randomUUID());

        var result = handler.handleMfaRequired(ex);

        assertThat(result).extracting("code", "status")
                .containsExactly(MFA_REQUIRED_CODE, "Unprocessable Entity");
    }

    @Test
    void should_return_forbidden_for_builtin_role_modification() {
        var ex = BuiltInRoleModificationException.forRole(UUID.randomUUID(), "ADMIN");

        var result = handler.handleBuiltInRoleModification(ex);

        assertThat(result).extracting("code", "status")
                .containsExactly(BUILTIN_ROLE_MODIFY_CODE, "Forbidden");
    }

    @Test
    void should_return_forbidden_for_last_admin() {
        var ex = LastAdminException.forMerchant(UUID.randomUUID());

        var result = handler.handleLastAdmin(ex);

        assertThat(result).extracting("code", "status")
                .containsExactly(LAST_ADMIN_CODE, "Forbidden");
    }

    @Test
    void should_return_not_found_for_user() {
        var ex = UserNotFoundException.withId(UUID.randomUUID());

        var result = handler.handleUserNotFound(ex);

        assertThat(result).extracting("code", "status")
                .containsExactly(USER_NOT_FOUND_CODE, "Not Found");
    }

    @Test
    void should_return_not_found_for_role() {
        var ex = RoleNotFoundException.withId(UUID.randomUUID());

        var result = handler.handleRoleNotFound(ex);

        assertThat(result).extracting("code", "status")
                .containsExactly(ROLE_NOT_FOUND_CODE, "Not Found");
    }

    @Test
    void should_return_not_found_for_invitation() {
        var ex = InvitationNotFoundException.withId(UUID.randomUUID());

        var result = handler.handleInvitationNotFound(ex);

        assertThat(result).extracting("code", "status")
                .containsExactly(INVITATION_NOT_FOUND_CODE, "Not Found");
    }

    @Test
    void should_return_conflict_for_user_already_exists() {
        var ex = UserAlreadyExistsException.forMerchant(UUID.randomUUID(), "test@example.com");

        var result = handler.handleUserAlreadyExists(ex);

        assertThat(result).extracting("code", "status")
                .containsExactly(USER_ALREADY_EXISTS_CODE, "Conflict");
    }

    @Test
    void should_return_conflict_for_role_in_use() {
        var ex = RoleInUseException.forRole(UUID.randomUUID(), 3);

        var result = handler.handleRoleInUse(ex);

        assertThat(result).extracting("code", "status")
                .containsExactly(ROLE_IN_USE_CODE, "Conflict");
    }

    @Test
    void should_return_gone_for_invitation_expired() {
        var ex = InvitationExpiredException.withId(UUID.randomUUID());

        var result = handler.handleInvitationExpired(ex);

        assertThat(result).extracting("code", "status")
                .containsExactly(INVITATION_EXPIRED_CODE, "Gone");
    }

    @Test
    void should_return_unprocessable_for_invalid_user_state() {
        var ex = InvalidUserStateException.forUser(UUID.randomUUID(), "SUSPENDED", "login");

        var result = handler.handleInvalidState(ex);

        assertThat(result).extracting("code", "status")
                .containsExactly(INVALID_USER_STATE_CODE, "Unprocessable Entity");
    }

    @Test
    void should_return_unprocessable_for_state_machine_exception() {
        var ex = StateMachineException.invalidTransition("ACTIVE", "ACTIVATE");

        var result = handler.handleInvalidState(ex);

        assertThat(result.code()).isEqualTo(INVALID_USER_STATE_CODE);
    }

    @Test
    void should_return_internal_error_for_unexpected_exception() {
        var ex = new RuntimeException("something broke");

        var result = handler.handleUnexpected(ex);

        assertThat(result).extracting("code", "status", "message")
                .containsExactly(INTERNAL_ERROR_CODE, "Internal Server Error", "Internal Server Error");
    }
}
