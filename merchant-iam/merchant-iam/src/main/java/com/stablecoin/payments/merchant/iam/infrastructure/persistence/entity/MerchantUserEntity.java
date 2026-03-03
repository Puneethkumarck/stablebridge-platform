package com.stablecoin.payments.merchant.iam.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "merchant_users")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MerchantUserEntity {

    @Id
    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(name = "merchant_id", nullable = false)
    private UUID merchantId;

    @Column(name = "email", nullable = false, columnDefinition = "bytea")
    private byte[] email;

    @Column(name = "email_hash", nullable = false, length = 64)
    private String emailHash;

    @Column(name = "full_name", nullable = false, length = 200)
    private String fullName;

    @Column(name = "status", nullable = false, length = 15)
    private String status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id", nullable = false)
    private RoleEntity role;

    @Column(name = "mfa_enabled", nullable = false)
    private boolean mfaEnabled;

    @Column(name = "mfa_secret_ref", length = 200)
    private String mfaSecretRef;

    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    @Column(name = "password_hash", length = 128)
    private String passwordHash;

    @Column(name = "auth_provider", nullable = false, length = 15)
    private String authProvider;

    @Column(name = "invited_by")
    private UUID invitedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "activated_at")
    private Instant activatedAt;

    @Column(name = "suspended_at")
    private Instant suspendedAt;

    @Column(name = "deactivated_at")
    private Instant deactivatedAt;

    @Version
    @Column(name = "version")
    private Long version;
}
