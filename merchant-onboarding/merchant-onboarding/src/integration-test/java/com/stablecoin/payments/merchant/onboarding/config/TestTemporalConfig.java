package com.stablecoin.payments.merchant.onboarding.config;

import com.stablecoin.payments.merchant.onboarding.infrastructure.temporal.workflow.MerchantOnboardingWorkflow;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Provides a mock WorkflowClient for integration tests. Temporal auto-configuration is excluded, so we need to supply
 * the bean ourselves. The mock is configured to return a no-op workflow stub for startKyb() calls.
 */
@TestConfiguration
public class TestTemporalConfig {

  @Bean
  public WorkflowClient workflowClient() {
    var client = mock(WorkflowClient.class);
    var workflowStub = mock(MerchantOnboardingWorkflow.class);
    given(client.newWorkflowStub(any(Class.class), any(WorkflowOptions.class))).willReturn(workflowStub);
    given(client.newWorkflowStub(any(Class.class), any(String.class))).willReturn(workflowStub);
    return client;
  }
}
