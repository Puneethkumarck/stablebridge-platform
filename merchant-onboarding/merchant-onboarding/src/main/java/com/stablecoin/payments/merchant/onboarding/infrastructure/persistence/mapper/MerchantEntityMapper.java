package com.stablecoin.payments.merchant.onboarding.infrastructure.persistence.mapper;

import com.stablecoin.payments.merchant.onboarding.domain.merchant.Merchant;
import com.stablecoin.payments.merchant.onboarding.domain.merchant.model.core.BeneficialOwner;
import com.stablecoin.payments.merchant.onboarding.domain.merchant.model.core.BusinessAddress;
import com.stablecoin.payments.merchant.onboarding.infrastructure.persistence.entity.MerchantEntity;
import com.stablecoin.payments.merchant.onboarding.infrastructure.persistence.entity.MerchantEntity.AddressJson;
import com.stablecoin.payments.merchant.onboarding.infrastructure.persistence.entity.MerchantEntity.BeneficialOwnerJson;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Mapper
public interface MerchantEntityMapper {

    @Mapping(target = "version", ignore = true)
    MerchantEntity toEntity(Merchant merchant);

    Merchant toDomain(MerchantEntity entity);

    default AddressJson toAddressJson(BusinessAddress address) {
        if (address == null) return null;
        return new AddressJson(
                address.streetLine1(), address.streetLine2(), address.city(),
                address.stateProvince(), address.postcode(), address.country());
    }

    default BusinessAddress toBusinessAddress(AddressJson json) {
        if (json == null) return null;
        return BusinessAddress.builder()
                .streetLine1(json.streetLine1()).streetLine2(json.streetLine2())
                .city(json.city()).stateProvince(json.stateProvince())
                .postcode(json.postcode()).country(json.country())
                .build();
    }

    default List<BeneficialOwnerJson> toBeneficialOwnerJsonList(List<BeneficialOwner> owners) {
        if (owners == null) return List.of();
        return owners.stream().map(this::toBeneficialOwnerJson).toList();
    }

    default BeneficialOwnerJson toBeneficialOwnerJson(BeneficialOwner owner) {
        if (owner == null) return null;
        return new BeneficialOwnerJson(
                owner.fullName(),
                owner.dateOfBirth() != null ? owner.dateOfBirth().toString() : null,
                owner.nationality(),
                owner.ownershipPct() != null ? owner.ownershipPct().toPlainString() : null,
                owner.isPoliticallyExposed(),
                owner.nationalIdRef());
    }

    default List<BeneficialOwner> toBeneficialOwnerList(List<BeneficialOwnerJson> jsons) {
        if (jsons == null) return List.of();
        return jsons.stream().map(this::toBeneficialOwner).toList();
    }

    default BeneficialOwner toBeneficialOwner(BeneficialOwnerJson json) {
        if (json == null) return null;
        return BeneficialOwner.builder()
                .fullName(json.fullName())
                .dateOfBirth(json.dateOfBirth() != null ? LocalDate.parse(json.dateOfBirth()) : null)
                .nationality(json.nationality())
                .ownershipPct(json.ownershipPct() != null ? new BigDecimal(json.ownershipPct()) : null)
                .isPoliticallyExposed(json.isPoliticallyExposed())
                .nationalIdRef(json.nationalIdRef())
                .build();
    }
}
