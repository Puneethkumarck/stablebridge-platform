package com.stablecoin.payments.merchant.onboarding.infrastructure.temporal.workflow;

import com.stablecoin.payments.merchant.onboarding.infrastructure.temporal.signal.DocumentUploadedSignal;
import com.stablecoin.payments.merchant.onboarding.infrastructure.temporal.signal.KybResultSignal;
import com.stablecoin.payments.merchant.onboarding.infrastructure.temporal.signal.ReviewDecisionSignal;
import io.temporal.workflow.QueryMethod;
import io.temporal.workflow.SignalMethod;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

import java.util.UUID;

@WorkflowInterface
public interface MerchantOnboardingWorkflow {

  @WorkflowMethod
  OnboardingResult runOnboarding(UUID merchantId);

  @SignalMethod
  void kybResultReceived(KybResultSignal signal);

  @SignalMethod
  void documentUploaded(DocumentUploadedSignal signal);

  @SignalMethod
  void reviewDecision(ReviewDecisionSignal signal);

  @QueryMethod
  String getOnboardingStatus();
}
