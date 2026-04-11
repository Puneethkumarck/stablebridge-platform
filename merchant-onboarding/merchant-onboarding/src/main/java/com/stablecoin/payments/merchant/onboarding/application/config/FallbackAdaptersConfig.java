package com.stablecoin.payments.merchant.onboarding.application.config;

import com.stablecoin.payments.merchant.onboarding.domain.merchant.CompanyRegistryProvider;
import com.stablecoin.payments.merchant.onboarding.domain.merchant.DocumentStore;
import com.stablecoin.payments.merchant.onboarding.domain.merchant.KybProvider;
import com.stablecoin.payments.merchant.onboarding.domain.merchant.OnboardingWorkflowPort;
import com.stablecoin.payments.merchant.onboarding.infrastructure.document.MockDocumentStoreAdapter;
import com.stablecoin.payments.merchant.onboarding.infrastructure.kyb.MockCompanyRegistryAdapter;
import com.stablecoin.payments.merchant.onboarding.infrastructure.kyb.MockKybAdapter;
import com.stablecoin.payments.merchant.onboarding.infrastructure.temporal.adapter.MockOnboardingWorkflowAdapter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "app.fallback-adapters.enabled", havingValue = "true")
public class FallbackAdaptersConfig {

  @Bean
  @ConditionalOnMissingBean
  KybProvider mockKybProvider() {
    return new MockKybAdapter();
  }

  @Bean
  @ConditionalOnMissingBean
  CompanyRegistryProvider mockCompanyRegistryProvider() {
    return new MockCompanyRegistryAdapter();
  }

  @Bean
  @ConditionalOnMissingBean
  DocumentStore mockDocumentStore() {
    return new MockDocumentStoreAdapter();
  }

  @Bean
  @ConditionalOnMissingBean
  OnboardingWorkflowPort mockOnboardingWorkflow() {
    return new MockOnboardingWorkflowAdapter();
  }
}
