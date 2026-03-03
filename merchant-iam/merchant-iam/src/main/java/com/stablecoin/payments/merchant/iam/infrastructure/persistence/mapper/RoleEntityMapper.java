package com.stablecoin.payments.merchant.iam.infrastructure.persistence.mapper;

import com.stablecoin.payments.merchant.iam.domain.team.model.Role;
import com.stablecoin.payments.merchant.iam.domain.team.model.core.Permission;
import com.stablecoin.payments.merchant.iam.infrastructure.persistence.entity.RoleEntity;
import com.stablecoin.payments.merchant.iam.infrastructure.persistence.entity.RolePermissionEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Mapper
public interface RoleEntityMapper {

    /**
     * Maps a Role domain object to a RoleEntity.
     * The permissions collection is deliberately ignored here;
     * the repository adapter populates permissions separately after saving.
     */
    @Mapping(target = "permissions", ignore = true)
    RoleEntity toEntityWithoutPermissions(Role role);

    @Mapping(target = "permissions", expression = "java(toPermissions(entity.getPermissions()))")
    Role toDomain(RoleEntity entity);

    default List<RolePermissionEntity> buildPermissionEntities(RoleEntity parent, List<Permission> permissions) {
        var result = new ArrayList<RolePermissionEntity>();
        for (var permission : permissions) {
            result.add(RolePermissionEntity.builder()
                    .rolePermissionId(UUID.randomUUID())
                    .role(parent)
                    .permission(permission.namespace() + ":" + permission.action())
                    .createdAt(Instant.now())
                    .build());
        }
        return result;
    }

    default List<Permission> toPermissions(List<RolePermissionEntity> entities) {
        if (entities == null) {
            return List.of();
        }
        return entities.stream()
                .map(e -> Permission.parse(e.getPermission()))
                .toList();
    }
}
