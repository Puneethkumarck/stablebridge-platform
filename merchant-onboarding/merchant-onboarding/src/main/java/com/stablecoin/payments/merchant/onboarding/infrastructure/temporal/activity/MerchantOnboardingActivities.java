package com.stablecoin.payments.merchant.onboarding.infrastructure.temporal.activity;

import com.stablecoin.payments.merchant.onboarding.infrastructure.temporal.signal.KybResultSignal;
import io.temporal.activity.ActivityInterface;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@ActivityInterface
public interface MerchantOnboardingActivities {

  String verifyCompanyRegistry(UUID merchantId);

  String startKyb(UUID merchantId);

  void processKybResult(UUID merchantId, KybResultSignal kybResult);

  String calculateRiskTier(Map<String, Object> riskSignals);

  void markKybPassed(UUID merchantId, String riskTier);

  void rejectMerchant(UUID merchantId, String reason);

  void notifyOpsTeam(UUID merchantId);

  void sendDocumentReminder(UUID merchantId, List<String> missingDocumentTypes);

  void escalateReview(UUID merchantId);
}
