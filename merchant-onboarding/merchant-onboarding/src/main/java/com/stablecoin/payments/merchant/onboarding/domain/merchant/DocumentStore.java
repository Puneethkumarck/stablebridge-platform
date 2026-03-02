package com.stablecoin.payments.merchant.onboarding.domain.merchant;

import java.util.UUID;

/**
 * Outbound port for document storage.
 * Infrastructure adapters generate pre-signed upload URLs; bytes never pass through the service.
 */
public interface DocumentStore {

    String generateUploadUrl(UUID merchantId, String documentType, String fileName);
}
