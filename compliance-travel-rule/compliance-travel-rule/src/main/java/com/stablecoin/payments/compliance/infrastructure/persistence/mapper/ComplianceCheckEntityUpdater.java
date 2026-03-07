package com.stablecoin.payments.compliance.infrastructure.persistence.mapper;

import com.stablecoin.payments.compliance.domain.model.ComplianceCheck;
import com.stablecoin.payments.compliance.infrastructure.persistence.entity.ComplianceCheckEntity;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(uses = ComplianceCheckPersistenceMapper.class)
public interface ComplianceCheckEntityUpdater {

    default void updateEntity(@MappingTarget ComplianceCheckEntity entity, ComplianceCheck check) {
        if (check == null) {
            return;
        }
        entity.setPaymentId(check.paymentId());
        entity.setCorrelationId(check.correlationId());
        entity.setSenderId(check.senderId());
        entity.setRecipientId(check.recipientId());
        entity.setSourceAmount(check.sourceAmount());
        entity.setSourceCurrency(check.sourceCurrency());
        entity.setTargetCurrency(check.targetCurrency());
        entity.setSourceCountry(check.sourceCountry());
        entity.setTargetCountry(check.targetCountry());
        entity.setStatus(check.status());
        entity.setOverallResult(check.overallResult());
        entity.setRiskScore(check.riskScore() != null ? check.riskScore().score() : null);
        entity.setRiskBand(check.riskScore() != null ? check.riskScore().band() : null);
        entity.setRiskFactors(check.riskScore() != null ? check.riskScore().factors() : null);
        entity.setErrorCode(check.errorCode());
        entity.setErrorMessage(check.errorMessage());
        entity.setCompletedAt(check.completedAt());
        entity.setExpiresAt(check.expiresAt());
    }
}
