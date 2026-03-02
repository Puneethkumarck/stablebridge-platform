package com.stablecoin.payments.merchant.onboarding.infrastructure.persistence.mapper;

import com.stablecoin.payments.merchant.onboarding.domain.merchant.Merchant;
import com.stablecoin.payments.merchant.onboarding.infrastructure.persistence.entity.MerchantEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(uses = MerchantEntityMapper.class)
public interface MerchantEntityUpdater {

    @Mapping(target = "version", ignore = true)
    @Mapping(target = "merchantId", ignore = true)
    void updateEntity(@MappingTarget MerchantEntity entity, Merchant merchant);
}
