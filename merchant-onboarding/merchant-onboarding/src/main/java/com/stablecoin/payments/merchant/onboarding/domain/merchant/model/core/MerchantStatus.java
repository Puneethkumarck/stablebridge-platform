package com.stablecoin.payments.merchant.onboarding.domain.merchant.model.core;

public enum MerchantStatus {
    APPLIED,
    KYB_IN_PROGRESS,
    KYB_MANUAL_REVIEW,
    KYB_REJECTED,
    PENDING_APPROVAL,
    ACTIVE,
    SUSPENDED,
    CLOSED
}
