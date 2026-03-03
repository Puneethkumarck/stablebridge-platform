package com.stablecoin.payments.merchant.iam.api;

import com.stablecoin.payments.merchant.iam.api.request.AcceptInvitationRequest;
import com.stablecoin.payments.merchant.iam.api.request.ChangeUserRoleRequest;
import com.stablecoin.payments.merchant.iam.api.request.CreateRoleRequest;
import com.stablecoin.payments.merchant.iam.api.request.DeactivateUserRequest;
import com.stablecoin.payments.merchant.iam.api.request.InviteUserRequest;
import com.stablecoin.payments.merchant.iam.api.request.LoginRequest;
import com.stablecoin.payments.merchant.iam.api.request.MfaVerifyRequest;
import com.stablecoin.payments.merchant.iam.api.request.RefreshTokenRequest;
import com.stablecoin.payments.merchant.iam.api.request.SuspendUserRequest;
import com.stablecoin.payments.merchant.iam.api.request.UpdateRoleRequest;
import com.stablecoin.payments.merchant.iam.api.response.ChangeRoleResponse;
import com.stablecoin.payments.merchant.iam.api.response.DataResponse;
import com.stablecoin.payments.merchant.iam.api.response.InvitationResponse;
import com.stablecoin.payments.merchant.iam.api.response.LoginResponse;
import com.stablecoin.payments.merchant.iam.api.response.MfaChallengeResponse;
import com.stablecoin.payments.merchant.iam.api.response.PageResponse;
import com.stablecoin.payments.merchant.iam.api.response.PermissionCheckResponse;
import com.stablecoin.payments.merchant.iam.api.response.ReactivateUserResponse;
import com.stablecoin.payments.merchant.iam.api.response.RoleResponse;
import com.stablecoin.payments.merchant.iam.api.response.RoleSummary;
import com.stablecoin.payments.merchant.iam.api.response.SuspendUserResponse;
import com.stablecoin.payments.merchant.iam.api.response.UserResponse;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ApiDtoTest {

    // ── Requests ─────────────────────────────────────────────────────────────

    @Test
    void login_request_stores_fields() {
        var req = new LoginRequest("user@example.com", "secret");
        assertThat(req).usingRecursiveComparison()
                .isEqualTo(new LoginRequest("user@example.com", "secret"));
    }

    @Test
    void mfa_verify_request_stores_fields() {
        var req = new MfaVerifyRequest("challenge-id", "123456");
        assertThat(req).usingRecursiveComparison()
                .isEqualTo(new MfaVerifyRequest("challenge-id", "123456"));
    }

    @Test
    void refresh_token_request_stores_token() {
        var req = new RefreshTokenRequest("rt_xyz");
        assertThat(req.refreshToken()).isEqualTo("rt_xyz");
    }

    @Test
    void invite_user_request_stores_fields() {
        var roleId = UUID.randomUUID();
        var req = new InviteUserRequest("alice@example.com", "Alice", roleId);
        assertThat(req).usingRecursiveComparison()
                .isEqualTo(new InviteUserRequest("alice@example.com", "Alice", roleId));
    }

    @Test
    void accept_invitation_request_stores_fields() {
        var req = new AcceptInvitationRequest("Alice Smith", "SecureP@ss123!");
        assertThat(req).usingRecursiveComparison()
                .isEqualTo(new AcceptInvitationRequest("Alice Smith", "SecureP@ss123!"));
    }

    @Test
    void change_role_request_stores_role_id() {
        var roleId = UUID.randomUUID();
        var req = new ChangeUserRoleRequest(roleId);
        assertThat(req.roleId()).isEqualTo(roleId);
    }

    @Test
    void suspend_user_request_stores_reason() {
        var req = new SuspendUserRequest("policy violation");
        assertThat(req.reason()).isEqualTo("policy violation");
    }

    @Test
    void deactivate_user_request_stores_reason() {
        var req = new DeactivateUserRequest("leaving company");
        assertThat(req.reason()).isEqualTo("leaving company");
    }

    @Test
    void create_role_request_stores_fields() {
        var req = new CreateRoleRequest("AUDITOR", "Read-only auditor", List.of("transactions:read"));
        assertThat(req).usingRecursiveComparison()
                .isEqualTo(new CreateRoleRequest("AUDITOR", "Read-only auditor", List.of("transactions:read")));
    }

    @Test
    void update_role_request_stores_permissions() {
        var req = new UpdateRoleRequest(List.of("transactions:read", "exports:read"));
        assertThat(req.permissions()).containsExactly("transactions:read", "exports:read");
    }

    // ── Responses ─────────────────────────────────────────────────────────────

    @Test
    void data_response_wraps_payload() {
        var inner = new RoleSummary(UUID.randomUUID(), "ADMIN");
        var wrapped = DataResponse.of(inner);
        assertThat(wrapped.data()).isSameAs(inner);
    }

    @Test
    void page_response_stores_metadata() {
        var items = List.of("a", "b");
        var expected = new PageResponse<>(items, new PageResponse.Page(0, 50, 2, 1));
        var page = new PageResponse<>(items, new PageResponse.Page(0, 50, 2, 1));
        assertThat(page).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void role_summary_stores_fields() {
        var roleId = UUID.randomUUID();
        var summary = new RoleSummary(roleId, "VIEWER");
        assertThat(summary).usingRecursiveComparison()
                .isEqualTo(new RoleSummary(roleId, "VIEWER"));
    }

    @Test
    void login_response_stores_all_fields() {
        var userId = UUID.randomUUID();
        var merchantId = UUID.randomUUID();
        var userInfo = new LoginResponse.UserInfo(userId, merchantId, "Alice", "ADMIN", List.of("*:*"));
        var expected = new LoginResponse("at_xyz", "rt_xyz", "Bearer", 3600, userInfo);
        var response = new LoginResponse("at_xyz", "rt_xyz", "Bearer", 3600, userInfo);
        assertThat(response).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void mfa_challenge_response_stores_fields() {
        var r = new MfaChallengeResponse(true, "chal-id", 300);
        assertThat(r).usingRecursiveComparison()
                .isEqualTo(new MfaChallengeResponse(true, "chal-id", 300));
    }

    @Test
    void permission_check_response_stores_fields() {
        var r = new PermissionCheckResponse(true, "ADMIN", "payments:*");
        assertThat(r).usingRecursiveComparison()
                .isEqualTo(new PermissionCheckResponse(true, "ADMIN", "payments:*"));
    }

    @Test
    void invitation_response_stores_fields() {
        var invId = UUID.randomUUID();
        var now = Instant.now();
        var r = new InvitationResponse(invId, "alice@example.com", "VIEWER", "PENDING", now, UUID.randomUUID(), now);
        assertThat(r.invitationId()).isEqualTo(invId);
        assertThat(r.status()).isEqualTo("PENDING");
    }

    @Test
    void user_response_stores_fields() {
        var userId = UUID.randomUUID();
        var role = new RoleSummary(UUID.randomUUID(), "ADMIN");
        var r = new UserResponse(userId, UUID.randomUUID(), "alice@example.com",
                "Alice", role, "ACTIVE", true, null, Instant.now(), null, Instant.now());
        assertThat(r.userId()).isEqualTo(userId);
        assertThat(r.role().roleName()).isEqualTo("ADMIN");
        assertThat(r.mfaEnabled()).isTrue();
    }

    @Test
    void change_role_response_stores_fields() {
        var userId = UUID.randomUUID();
        var changedBy = UUID.randomUUID();
        var now = Instant.now();
        var r = new ChangeRoleResponse(userId, "VIEWER", "ADMIN", now, changedBy);
        assertThat(r.oldRole()).isEqualTo("VIEWER");
        assertThat(r.newRole()).isEqualTo("ADMIN");
        assertThat(r.changedBy()).isEqualTo(changedBy);
    }

    @Test
    void suspend_user_response_stores_fields() {
        var userId = UUID.randomUUID();
        var now = Instant.now();
        var r = new SuspendUserResponse(userId, "SUSPENDED", now);
        assertThat(r.status()).isEqualTo("SUSPENDED");
        assertThat(r.suspendedAt()).isEqualTo(now);
    }

    @Test
    void reactivate_user_response_stores_fields() {
        var userId = UUID.randomUUID();
        var now = Instant.now();
        var r = new ReactivateUserResponse(userId, "ACTIVE", now);
        assertThat(r.status()).isEqualTo("ACTIVE");
    }

    @Test
    void role_response_stores_fields() {
        var roleId = UUID.randomUUID();
        var now = Instant.now();
        var r = new RoleResponse(roleId, "FINANCE_AUDITOR", "Read-only finance", false, true, 2L,
                List.of("transactions:read", "exports:read"), now, now);
        assertThat(r.roleName()).isEqualTo("FINANCE_AUDITOR");
        assertThat(r.builtin()).isFalse();
        assertThat(r.userCount()).isEqualTo(2);
        assertThat(r.permissions()).containsExactly("transactions:read", "exports:read");
    }
}
