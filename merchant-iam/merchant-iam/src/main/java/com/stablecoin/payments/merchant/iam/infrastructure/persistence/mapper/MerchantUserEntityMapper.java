package com.stablecoin.payments.merchant.iam.infrastructure.persistence.mapper;

import com.stablecoin.payments.merchant.iam.domain.team.model.MerchantUser;
import com.stablecoin.payments.merchant.iam.infrastructure.persistence.entity.MerchantUserEntity;
import com.stablecoin.payments.merchant.iam.infrastructure.persistence.entity.RoleEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Mapper
public interface MerchantUserEntityMapper {

    /**
     * Domain → Entity.
     * email is encrypted/stored as bytea; role FK requires a pre-loaded RoleEntity.
     */
    @Mapping(target = "email", expression = "java(toBytes(user.email()))")
    @Mapping(target = "role", expression = "java(roleRef(user.roleId()))")
    @Mapping(target = "status", expression = "java(user.status().name())")
    @Mapping(target = "authProvider", expression = "java(user.authProvider().name())")
    @Mapping(target = "version", ignore = true)
    MerchantUserEntity toEntity(MerchantUser user);

    /**
     * Entity → Domain.
     * roleId is extracted from the role FK; email decoded from bytea.
     */
    @Mapping(target = "email", expression = "java(fromBytes(entity.getEmail()))")
    @Mapping(target = "roleId", expression = "java(entity.getRole().getRoleId())")
    @Mapping(target = "status", expression = "java(com.stablecoin.payments.merchant.iam.domain.team.model.core.UserStatus.valueOf(entity.getStatus()))")
    @Mapping(target = "authProvider", expression = "java(com.stablecoin.payments.merchant.iam.domain.team.model.core.AuthProvider.valueOf(entity.getAuthProvider()))")
    MerchantUser toDomain(MerchantUserEntity entity);

    default byte[] toBytes(String value) {
        if (value == null) {
            return null;
        }
        return value.getBytes(StandardCharsets.UTF_8);
    }

    default String fromBytes(byte[] bytes) {
        if (bytes == null) {
            return null;
        }
        return new String(bytes, StandardCharsets.UTF_8);
    }

    default RoleEntity roleRef(UUID roleId) {
        var entity = new RoleEntity();
        entity.setRoleId(roleId);
        return entity;
    }
}
