package com.stablecoin.payments.merchant.onboarding.domain.merchant;

import com.stablecoin.payments.merchant.onboarding.domain.merchant.model.core.DocumentType;
import com.stablecoin.payments.merchant.onboarding.domain.merchant.model.core.KybVerification;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface KybProvider {

    KybVerification submit(UUID merchantId, String legalName, String registrationNumber, String country);

    Optional<KybVerification> getResult(String providerRef);

    KybVerification handleWebhook(Map<String, Object> payload);

    List<DocumentType> getRequiredDocuments(String country, String entityType);
}
