package com.stablecoin.payments.merchant.onboarding.domain.merchant;

public enum MerchantTrigger {
    START_KYB,
    KYB_PASSED,
    KYB_FLAGGED,
    KYB_FAILED,
    APPROVE,
    SUSPEND,
    REACTIVATE,
    CLOSE
}
