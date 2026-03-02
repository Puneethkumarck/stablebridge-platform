package com.stablecoin.payments.merchant.iam.fixtures;

import com.stablecoin.payments.merchant.iam.infrastructure.persistence.entity.InvitationEntity;
import com.stablecoin.payments.merchant.iam.infrastructure.persistence.entity.MerchantUserEntity;
import com.stablecoin.payments.merchant.iam.infrastructure.persistence.entity.PermissionAuditLogEntity;
import com.stablecoin.payments.merchant.iam.infrastructure.persistence.entity.RoleEntity;
import com.stablecoin.payments.merchant.iam.infrastructure.persistence.entity.RolePermissionEntity;
import com.stablecoin.payments.merchant.iam.infrastructure.persistence.entity.UserSessionEntity;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

public final class IamEntityFixtures {

    private static final UUID MERCHANT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID ADMIN_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000010");

    private IamEntityFixtures() {}

    public static UUID defaultMerchantId() {
        return MERCHANT_ID;
    }

    // ─── Roles ───

    public static RoleEntity anAdminRole() {
        return RoleEntity.builder()
                .roleId(UUID.randomUUID())
                .merchantId(MERCHANT_ID)
                .roleName("ADMIN")
                .description("Full access")
                .builtin(true)
                .active(true)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    public static RoleEntity aViewerRole() {
        return RoleEntity.builder()
                .roleId(UUID.randomUUID())
                .merchantId(MERCHANT_ID)
                .roleName("VIEWER")
                .description("Read-only access")
                .builtin(true)
                .active(true)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    public static RoleEntity anInactiveRole() {
        return RoleEntity.builder()
                .roleId(UUID.randomUUID())
                .merchantId(MERCHANT_ID)
                .roleName("DEPRECATED_ROLE")
                .description("No longer in use")
                .builtin(false)
                .active(false)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    // ─── Role Permissions ───

    public static RolePermissionEntity aRolePermission(RoleEntity role, String permission) {
        return RolePermissionEntity.builder()
                .rolePermissionId(UUID.randomUUID())
                .role(role)
                .permission(permission)
                .createdAt(Instant.now())
                .build();
    }

    // ─── Merchant Users ───

    public static MerchantUserEntity anActiveUser(RoleEntity role) {
        return MerchantUserEntity.builder()
                .userId(UUID.randomUUID())
                .merchantId(MERCHANT_ID)
                .email("admin@example.com".getBytes(StandardCharsets.UTF_8))
                .emailHash("a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2")
                .fullName("Admin User")
                .status("ACTIVE")
                .role(role)
                .mfaEnabled(false)
                .authProvider("LOCAL")
                .passwordHash("$2a$10$dummyhashvaluefortest")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .activatedAt(Instant.now())
                .build();
    }

    public static MerchantUserEntity anInvitedUser(RoleEntity role) {
        return MerchantUserEntity.builder()
                .userId(UUID.randomUUID())
                .merchantId(MERCHANT_ID)
                .email("invited@example.com".getBytes(StandardCharsets.UTF_8))
                .emailHash("b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3")
                .fullName("Invited User")
                .status("INVITED")
                .role(role)
                .mfaEnabled(false)
                .authProvider("LOCAL")
                .invitedBy(ADMIN_USER_ID)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    // ─── Invitations ───

    public static InvitationEntity aPendingInvitation(RoleEntity role) {
        return InvitationEntity.builder()
                .invitationId(UUID.randomUUID())
                .merchantId(MERCHANT_ID)
                .email("newuser@example.com".getBytes(StandardCharsets.UTF_8))
                .emailHash("c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4")
                .role(role)
                .invitedBy(ADMIN_USER_ID)
                .tokenHash("tokenhash_" + UUID.randomUUID())
                .status("PENDING")
                .createdAt(Instant.now())
                .expiresAt(Instant.now().plus(7, ChronoUnit.DAYS))
                .build();
    }

    public static InvitationEntity anExpiredInvitation(RoleEntity role) {
        return InvitationEntity.builder()
                .invitationId(UUID.randomUUID())
                .merchantId(MERCHANT_ID)
                .email("expired@example.com".getBytes(StandardCharsets.UTF_8))
                .emailHash("d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5")
                .role(role)
                .invitedBy(ADMIN_USER_ID)
                .tokenHash("tokenhash_expired_" + UUID.randomUUID())
                .status("PENDING")
                .createdAt(Instant.now().minus(10, ChronoUnit.DAYS))
                .expiresAt(Instant.now().minus(3, ChronoUnit.DAYS))
                .build();
    }

    // ─── User Sessions ───

    public static UserSessionEntity anActiveSession(MerchantUserEntity user) {
        return UserSessionEntity.builder()
                .sessionId(UUID.randomUUID())
                .user(user)
                .merchantId(user.getMerchantId())
                .ipAddress("192.168.1.1")
                .userAgent("Mozilla/5.0")
                .createdAt(Instant.now())
                .expiresAt(Instant.now().plus(24, ChronoUnit.HOURS))
                .lastActiveAt(Instant.now())
                .revoked(false)
                .build();
    }

    // ─── Permission Audit Log ───

    public static PermissionAuditLogEntity anAuditLogEntry() {
        return PermissionAuditLogEntity.builder()
                .logId(UUID.randomUUID())
                .merchantId(MERCHANT_ID)
                .userId(ADMIN_USER_ID)
                .targetUserId(UUID.randomUUID())
                .action("ROLE_CHANGED")
                .detail("{\"from\":\"VIEWER\",\"to\":\"ADMIN\"}")
                .ipAddress("10.0.0.1")
                .occurredAt(Instant.now())
                .build();
    }
}
