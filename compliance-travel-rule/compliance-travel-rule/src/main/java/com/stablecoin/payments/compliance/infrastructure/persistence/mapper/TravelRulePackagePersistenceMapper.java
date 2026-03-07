package com.stablecoin.payments.compliance.infrastructure.persistence.mapper;

import com.stablecoin.payments.compliance.domain.model.TravelRulePackage;
import com.stablecoin.payments.compliance.domain.model.VaspInfo;
import com.stablecoin.payments.compliance.infrastructure.persistence.entity.TravelRulePackageEntity;
import com.stablecoin.payments.compliance.infrastructure.persistence.entity.TravelRulePackageEntity.VaspInfoJson;
import org.mapstruct.Mapper;

import java.nio.charset.StandardCharsets;

@Mapper
public interface TravelRulePackagePersistenceMapper {

    default TravelRulePackageEntity toEntity(TravelRulePackage travelRulePackage) {
        if (travelRulePackage == null) {
            return null;
        }
        return TravelRulePackageEntity.builder()
                .packageId(travelRulePackage.packageId())
                .checkId(travelRulePackage.checkId())
                .originatorVasp(toVaspInfoJson(travelRulePackage.originatorVasp()))
                .beneficiaryVasp(toVaspInfoJson(travelRulePackage.beneficiaryVasp()))
                .originatorData(travelRulePackage.originatorData() != null
                        ? travelRulePackage.originatorData().getBytes(StandardCharsets.UTF_8) : null)
                .beneficiaryData(travelRulePackage.beneficiaryData() != null
                        ? travelRulePackage.beneficiaryData().getBytes(StandardCharsets.UTF_8) : null)
                .protocol(travelRulePackage.protocol())
                .transmissionStatus(travelRulePackage.transmissionStatus())
                .transmittedAt(travelRulePackage.transmittedAt())
                .protocolRef(travelRulePackage.protocolRef())
                .build();
    }

    default TravelRulePackage toDomain(TravelRulePackageEntity entity) {
        if (entity == null) {
            return null;
        }
        return new TravelRulePackage(
                entity.getPackageId(),
                entity.getCheckId(),
                toVaspInfo(entity.getOriginatorVasp()),
                toVaspInfo(entity.getBeneficiaryVasp()),
                entity.getOriginatorData() != null
                        ? new String(entity.getOriginatorData(), StandardCharsets.UTF_8) : null,
                entity.getBeneficiaryData() != null
                        ? new String(entity.getBeneficiaryData(), StandardCharsets.UTF_8) : null,
                entity.getProtocol(),
                entity.getTransmissionStatus(),
                entity.getTransmittedAt(),
                entity.getProtocolRef()
        );
    }

    default VaspInfo toVaspInfo(VaspInfoJson json) {
        if (json == null) {
            return null;
        }
        return new VaspInfo(json.vaspId(), json.name(), json.country(), json.did());
    }

    default VaspInfoJson toVaspInfoJson(VaspInfo info) {
        if (info == null) {
            return null;
        }
        return new VaspInfoJson(info.vaspId(), info.name(), info.country(), info.did());
    }
}
