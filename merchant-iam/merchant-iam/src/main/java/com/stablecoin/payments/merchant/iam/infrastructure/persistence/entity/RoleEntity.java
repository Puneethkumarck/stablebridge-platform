package com.stablecoin.payments.merchant.iam.infrastructure.persistence.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "roles")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoleEntity {

    @Id
    @Column(name = "role_id", nullable = false, updatable = false)
    private UUID roleId;

    @Column(name = "merchant_id", nullable = false)
    private UUID merchantId;

    @Column(name = "role_name", nullable = false, length = 50)
    private String roleName;

    @Column(name = "description")
    private String description;

    @Column(name = "is_builtin", nullable = false)
    private boolean builtin;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "role", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<RolePermissionEntity> permissions = new ArrayList<>();
}
