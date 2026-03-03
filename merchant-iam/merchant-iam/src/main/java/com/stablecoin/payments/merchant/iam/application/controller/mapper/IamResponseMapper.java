package com.stablecoin.payments.merchant.iam.application.controller.mapper;

import com.stablecoin.payments.merchant.iam.api.response.ChangeRoleResponse;
import com.stablecoin.payments.merchant.iam.api.response.InvitationResponse;
import com.stablecoin.payments.merchant.iam.api.response.ReactivateUserResponse;
import com.stablecoin.payments.merchant.iam.api.response.RoleResponse;
import com.stablecoin.payments.merchant.iam.api.response.RoleSummary;
import com.stablecoin.payments.merchant.iam.api.response.SuspendUserResponse;
import com.stablecoin.payments.merchant.iam.api.response.UserResponse;
import com.stablecoin.payments.merchant.iam.domain.team.RoleChangeResult;
import com.stablecoin.payments.merchant.iam.domain.team.model.Invitation;
import com.stablecoin.payments.merchant.iam.domain.team.model.MerchantUser;
import com.stablecoin.payments.merchant.iam.domain.team.model.Role;
import com.stablecoin.payments.merchant.iam.domain.team.model.core.Permission;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.time.Instant;
import java.util.List;

@Mapper
public interface IamResponseMapper {

    // ── Role ─────────────────────────────────────────────────────────────────

    RoleSummary toRoleSummary(Role role);

    @Mapping(target = "permissions", expression = "java(toPermissionStrings(role.permissions()))")
    @Mapping(target = "userCount", constant = "0L")
    RoleResponse toRoleResponse(Role role);

    // ── User ─────────────────────────────────────────────────────────────────

    default UserResponse toUserResponse(MerchantUser user, Role role) {
        return new UserResponse(
                user.userId(),
                user.merchantId(),
                user.email(),
                user.fullName(),
                new RoleSummary(role.roleId(), role.roleName()),
                user.status().name(),
                user.mfaEnabled(),
                user.lastLoginAt(),
                user.activatedAt(),
                user.suspendedAt(),
                user.createdAt());
    }

    // ── Invitation ────────────────────────────────────────────────────────────

    default InvitationResponse toInvitationResponse(Invitation invitation, String roleName) {
        return new InvitationResponse(
                invitation.invitationId(),
                invitation.email(),
                roleName,
                invitation.status().name(),
                invitation.expiresAt(),
                invitation.invitedBy(),
                invitation.createdAt());
    }

    // ── State-change responses ────────────────────────────────────────────────

    default ChangeRoleResponse toChangeRoleResponse(RoleChangeResult result) {
        return new ChangeRoleResponse(
                result.user().userId(),
                result.previousRoleName(),
                result.newRoleName(),
                Instant.now(),
                result.changedBy());
    }

    default SuspendUserResponse toSuspendUserResponse(MerchantUser user) {
        return new SuspendUserResponse(user.userId(), user.status().name(), user.suspendedAt());
    }

    default ReactivateUserResponse toReactivateUserResponse(MerchantUser user) {
        return new ReactivateUserResponse(user.userId(), user.status().name(), user.activatedAt());
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    default List<String> toPermissionStrings(List<Permission> permissions) {
        if (permissions == null) {
            return List.of();
        }
        return permissions.stream().map(Permission::toString).toList();
    }
}
