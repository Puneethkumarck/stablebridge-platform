package com.stablecoin.payments.orchestrator.domain.workflow.activity;

import io.temporal.activity.ActivityInterface;

@ActivityInterface
public interface ComplianceCheckActivity {

    ComplianceResult checkCompliance(ComplianceRequest request);
}
