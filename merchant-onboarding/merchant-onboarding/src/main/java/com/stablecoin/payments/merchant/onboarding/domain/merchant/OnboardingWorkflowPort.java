package com.stablecoin.payments.merchant.onboarding.domain.merchant;

import java.util.UUID;

public interface OnboardingWorkflowPort {

    void startOnboarding(UUID merchantId);
}
