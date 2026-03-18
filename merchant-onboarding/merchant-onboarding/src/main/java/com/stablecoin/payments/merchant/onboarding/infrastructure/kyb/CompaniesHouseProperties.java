package com.stablecoin.payments.merchant.onboarding.infrastructure.kyb;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.company-registry.companies-house")
public record CompaniesHouseProperties(String baseUrl, String apiKey, Integer connectionTimeoutMs,
    Integer readTimeoutMs) {

  public CompaniesHouseProperties {
    if (baseUrl == null || baseUrl.isBlank()) {
      baseUrl = "https://api.company-information.service.gov.uk";
    }
    if (connectionTimeoutMs == null || connectionTimeoutMs <= 0) {
      connectionTimeoutMs = 5000;
    }
    if (readTimeoutMs == null || readTimeoutMs <= 0) {
      readTimeoutMs = 10000;
    }
  }
}
