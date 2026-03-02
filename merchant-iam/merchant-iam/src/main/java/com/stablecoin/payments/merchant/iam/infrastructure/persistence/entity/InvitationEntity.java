package com.stablecoin.payments.merchant.iam.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "invitations")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvitationEntity {

    @Id
    @Column(name = "invitation_id", nullable = false, updatable = false)
    private UUID invitationId;

    @Column(name = "merchant_id", nullable = false)
    private UUID merchantId;

    @Column(name = "email", nullable = false, columnDefinition = "bytea")
    private byte[] email;

    @Column(name = "email_hash", nullable = false, length = 64)
    private String emailHash;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id", nullable = false)
    private RoleEntity role;

    @Column(name = "invited_by", nullable = false)
    private UUID invitedBy;

    @Column(name = "token_hash", nullable = false, length = 128)
    private String tokenHash;

    @Column(name = "status", nullable = false, length = 10)
    private String status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "accepted_at")
    private Instant acceptedAt;
}
