package com.stablecoin.payments.merchant.iam.infrastructure.persistence.repository;

import com.stablecoin.payments.merchant.iam.infrastructure.persistence.entity.RolePermissionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface RolePermissionJpaRepository extends JpaRepository<RolePermissionEntity, UUID> {

    List<RolePermissionEntity> findByRole_RoleId(UUID roleId);

    void deleteByRole_RoleId(UUID roleId);
}
