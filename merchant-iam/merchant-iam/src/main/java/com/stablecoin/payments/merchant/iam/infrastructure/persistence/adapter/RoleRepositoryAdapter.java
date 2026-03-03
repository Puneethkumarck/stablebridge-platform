package com.stablecoin.payments.merchant.iam.infrastructure.persistence.adapter;

import com.stablecoin.payments.merchant.iam.domain.team.RoleRepository;
import com.stablecoin.payments.merchant.iam.domain.team.model.Role;
import com.stablecoin.payments.merchant.iam.infrastructure.persistence.mapper.RoleEntityMapper;
import com.stablecoin.payments.merchant.iam.infrastructure.persistence.repository.RoleJpaRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Repository
@RequiredArgsConstructor
public class RoleRepositoryAdapter implements RoleRepository {

    private final RoleJpaRepository jpa;
    private final RoleEntityMapper mapper;
    private final EntityManager entityManager;

    @Override
    public Optional<Role> findById(UUID roleId) {
        return jpa.findById(roleId).map(mapper::toDomain);
    }

    @Override
    public Optional<Role> findByMerchantIdAndRoleName(UUID merchantId, String roleName) {
        return jpa.findByMerchantIdAndRoleName(merchantId, roleName).map(mapper::toDomain);
    }

    @Override
    public List<Role> findByMerchantId(UUID merchantId) {
        return jpa.findByMerchantIdAndActiveTrue(merchantId).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public long countActiveUsersByRoleId(UUID roleId) {
        return 0L;
    }

    @Override
    @Transactional
    public Role save(Role role) {
        var existing = jpa.findById(role.roleId());
        if (existing.isPresent()) {
            var entity = existing.get();
            entity.setRoleName(role.roleName());
            entity.setDescription(role.description());
            entity.setBuiltin(role.builtin());
            entity.setActive(role.active());
            entity.setCreatedBy(role.createdBy());
            entity.setUpdatedAt(role.updatedAt());

            // Determine whether permissions have changed
            var incomingPerms = role.permissions().stream()
                    .map(p -> p.namespace() + ":" + p.action())
                    .collect(Collectors.toSet());
            var existingPerms = entity.getPermissions().stream()
                    .map(rp -> rp.getPermission())
                    .collect(Collectors.toSet());

            if (!incomingPerms.equals(existingPerms)) {
                // Clear the managed collection so orphanRemoval issues DELETEs,
                // flush to push DELETEs to DB before new INSERTs (avoids unique constraint violation),
                // then add the replacement permission entities.
                entity.getPermissions().clear();
                entityManager.flush();
                entity.getPermissions().addAll(mapper.buildPermissionEntities(entity, role.permissions()));
            }

            // Save non-permission fields; the updated permissions collection is cascade-flushed.
            jpa.save(entity);
            // Evict and reload to get a clean domain object with the persisted permissions.
            entityManager.flush();
            entityManager.refresh(entity);
            return mapper.toDomain(entity);
        }
        var entity = mapper.toEntityWithoutPermissions(role);
        entity.getPermissions().addAll(mapper.buildPermissionEntities(entity, role.permissions()));
        return mapper.toDomain(jpa.save(entity));
    }

    @Override
    public List<Role> saveAll(List<Role> roles) {
        return roles.stream().map(this::save).toList();
    }
}
