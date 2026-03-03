package com.stablecoin.payments.merchant.iam.infrastructure.persistence.mapper;

import com.stablecoin.payments.merchant.iam.domain.team.model.Invitation;
import com.stablecoin.payments.merchant.iam.infrastructure.persistence.entity.InvitationEntity;
import com.stablecoin.payments.merchant.iam.infrastructure.persistence.entity.RoleEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Mapper
public interface InvitationEntityMapper {

    @Mapping(target = "email", expression = "java(toBytes(invitation.email()))")
    @Mapping(target = "role", expression = "java(roleRef(invitation.roleId()))")
    @Mapping(target = "status", expression = "java(invitation.status().name())")
    InvitationEntity toEntity(Invitation invitation);

    @Mapping(target = "email", expression = "java(fromBytes(entity.getEmail()))")
    @Mapping(target = "roleId", expression = "java(entity.getRole().getRoleId())")
    @Mapping(target = "status", expression = "java(com.stablecoin.payments.merchant.iam.domain.team.model.core.InvitationStatus.valueOf(entity.getStatus()))")
    Invitation toDomain(InvitationEntity entity);

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
