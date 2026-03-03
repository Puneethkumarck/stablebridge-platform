package com.stablecoin.payments.merchant.iam.infrastructure.persistence.mapper;

import com.stablecoin.payments.merchant.iam.domain.team.model.UserSession;
import com.stablecoin.payments.merchant.iam.infrastructure.persistence.entity.MerchantUserEntity;
import com.stablecoin.payments.merchant.iam.infrastructure.persistence.entity.UserSessionEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.UUID;

@Mapper
public interface UserSessionEntityMapper {

    @Mapping(target = "user", expression = "java(userRef(session.userId()))")
    UserSessionEntity toEntity(UserSession session);

    @Mapping(target = "userId", expression = "java(entity.getUser().getUserId())")
    UserSession toDomain(UserSessionEntity entity);

    default MerchantUserEntity userRef(UUID userId) {
        var entity = new MerchantUserEntity();
        entity.setUserId(userId);
        return entity;
    }
}
