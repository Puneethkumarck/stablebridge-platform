package com.stablecoin.payments.compliance.infrastructure.persistence.mapper;

import com.stablecoin.payments.compliance.domain.model.AmlResult;
import com.stablecoin.payments.compliance.domain.model.ComplianceCheck;
import com.stablecoin.payments.compliance.domain.model.KycResult;
import com.stablecoin.payments.compliance.domain.model.RiskScore;
import com.stablecoin.payments.compliance.domain.model.SanctionsResult;
import com.stablecoin.payments.compliance.domain.model.TravelRulePackage;
import com.stablecoin.payments.compliance.domain.model.VaspInfo;
import com.stablecoin.payments.compliance.infrastructure.persistence.entity.AmlResultEntity;
import com.stablecoin.payments.compliance.infrastructure.persistence.entity.ComplianceCheckEntity;
import com.stablecoin.payments.compliance.infrastructure.persistence.entity.KycResultEntity;
import com.stablecoin.payments.compliance.infrastructure.persistence.entity.SanctionsResultEntity;
import com.stablecoin.payments.compliance.infrastructure.persistence.entity.TravelRulePackageEntity;
import com.stablecoin.payments.compliance.infrastructure.persistence.entity.TravelRulePackageEntity.VaspInfoJson;
import org.mapstruct.Mapper;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Mapper
public interface ComplianceCheckPersistenceMapper {

    default ComplianceCheckEntity toEntity(ComplianceCheck check) {
        if (check == null) {
            return null;
        }
        return ComplianceCheckEntity.builder()
                .checkId(check.checkId())
                .paymentId(check.paymentId())
                .correlationId(check.correlationId())
                .senderId(check.senderId())
                .recipientId(check.recipientId())
                .sourceAmount(check.sourceAmount())
                .sourceCurrency(check.sourceCurrency())
                .targetCurrency(check.targetCurrency())
                .sourceCountry(check.sourceCountry())
                .targetCountry(check.targetCountry())
                .status(check.status())
                .overallResult(check.overallResult())
                .riskScore(check.riskScore() != null ? check.riskScore().score() : null)
                .riskBand(check.riskScore() != null ? check.riskScore().band() : null)
                .riskFactors(check.riskScore() != null ? check.riskScore().factors() : null)
                .errorCode(check.errorCode())
                .errorMessage(check.errorMessage())
                .createdAt(check.createdAt())
                .completedAt(check.completedAt())
                .expiresAt(check.expiresAt())
                .build();
    }

    default ComplianceCheck toDomain(ComplianceCheckEntity entity,
                                     KycResultEntity kycResult,
                                     SanctionsResultEntity sanctionsResult,
                                     AmlResultEntity amlResult,
                                     TravelRulePackageEntity travelRule) {
        if (entity == null) {
            return null;
        }

        RiskScore riskScore = null;
        if (entity.getRiskScore() != null) {
            riskScore = new RiskScore(
                    entity.getRiskScore(),
                    entity.getRiskBand(),
                    entity.getRiskFactors() != null ? entity.getRiskFactors() : List.of()
            );
        }

        return new ComplianceCheck(
                entity.getCheckId(),
                entity.getPaymentId(),
                entity.getCorrelationId(),
                entity.getSenderId(),
                entity.getRecipientId(),
                entity.getSourceAmount(),
                entity.getSourceCurrency(),
                entity.getTargetCurrency(),
                entity.getSourceCountry(),
                entity.getTargetCountry(),
                entity.getStatus(),
                entity.getOverallResult(),
                riskScore,
                kycResult != null ? toKycResult(kycResult) : null,
                sanctionsResult != null ? toSanctionsResult(sanctionsResult) : null,
                amlResult != null ? toAmlResult(amlResult) : null,
                travelRule != null ? toTravelRulePackage(travelRule) : null,
                entity.getErrorCode(),
                entity.getErrorMessage(),
                entity.getCreatedAt(),
                entity.getCompletedAt(),
                entity.getExpiresAt()
        );
    }

    default KycResult toKycResult(KycResultEntity entity) {
        if (entity == null) {
            return null;
        }
        return new KycResult(
                entity.getKycResultId(),
                entity.getCheckId(),
                entity.getSenderKycTier(),
                entity.getSenderStatus(),
                entity.getRecipientStatus(),
                entity.getProvider(),
                entity.getProviderRef(),
                entity.getCheckedAt()
        );
    }

    default SanctionsResult toSanctionsResult(SanctionsResultEntity entity) {
        if (entity == null) {
            return null;
        }
        return new SanctionsResult(
                entity.getSanctionsResultId(),
                entity.getCheckId(),
                entity.isSenderScreened(),
                entity.isRecipientScreened(),
                entity.isSenderHit(),
                entity.isRecipientHit(),
                entity.getHitDetails(),
                entity.getListsChecked(),
                entity.getProvider(),
                entity.getProviderRef(),
                entity.getScreenedAt()
        );
    }

    default AmlResult toAmlResult(AmlResultEntity entity) {
        if (entity == null) {
            return null;
        }
        return new AmlResult(
                entity.getAmlResultId(),
                entity.getCheckId(),
                entity.isFlagged(),
                entity.getFlagReasons(),
                entity.getChainAnalysis(),
                entity.getProvider(),
                entity.getProviderRef(),
                entity.getScreenedAt()
        );
    }

    default TravelRulePackage toTravelRulePackage(TravelRulePackageEntity entity) {
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
