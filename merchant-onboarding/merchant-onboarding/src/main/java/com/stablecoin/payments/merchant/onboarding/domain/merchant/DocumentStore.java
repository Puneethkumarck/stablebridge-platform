package com.stablecoin.payments.merchant.onboarding.domain.merchant;

import java.util.UUID;

public interface DocumentStore {

    String generateUploadUrl(UUID merchantId, String documentType, String fileName);
}
