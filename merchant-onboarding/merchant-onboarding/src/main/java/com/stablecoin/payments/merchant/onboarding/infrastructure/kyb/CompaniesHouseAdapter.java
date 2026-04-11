package com.stablecoin.payments.merchant.onboarding.infrastructure.kyb;

import com.stablecoin.payments.merchant.onboarding.domain.merchant.CompanyRegistryProvider;
import com.stablecoin.payments.platform.infrastructure.http.ExternalApiLoggingInterceptor;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.net.http.HttpClient.Version;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.Optional;

import static com.stablecoin.payments.platform.infrastructure.http.ExternalApiLoggingInterceptor.applyTo;

@Slf4j
@Component
@ConditionalOnProperty(name = "app.company-registry.provider", havingValue = "companies-house")
@EnableConfigurationProperties(CompaniesHouseProperties.class)
public class CompaniesHouseAdapter implements CompanyRegistryProvider {

  private final RestClient restClient;

  public CompaniesHouseAdapter(CompaniesHouseProperties properties,
                               @Nullable ExternalApiLoggingInterceptor loggingInterceptor) {
    var basicAuth = Base64.getEncoder()
        .encodeToString((properties.apiKey() + ":").getBytes(StandardCharsets.UTF_8));

    var httpClient = HttpClient.newBuilder()
        .version(Version.HTTP_1_1)
        .connectTimeout(Duration.ofMillis(properties.connectionTimeoutMs()))
        .build();

    var requestFactory = new JdkClientHttpRequestFactory(httpClient);
    requestFactory.setReadTimeout(Duration.ofMillis(properties.readTimeoutMs()));

    this.restClient = applyTo(RestClient.builder()
        .baseUrl(properties.baseUrl())
        .requestFactory(requestFactory)
        .defaultHeader(HttpHeaders.AUTHORIZATION, "Basic " + basicAuth), loggingInterceptor)
        .build();
  }

  @Override
  @Bulkhead(name = "companiesHouse")
  @CircuitBreaker(name = "companiesHouse", fallbackMethod = "lookupFallback")
  public Optional<CompanyProfile> lookup(String registrationNumber, String country) {
    if (!"GB".equalsIgnoreCase(country)) {
      log.debug("[COMPANIES-HOUSE] Skipping non-GB country={}", country);
      return Optional.empty();
    }

    log.info("[COMPANIES-HOUSE] Looking up company registrationNumber={}", registrationNumber);

    try {
      var response = restClient.get()
          .uri("/company/{companyNumber}", registrationNumber)
          .retrieve()
          .body(CompaniesHouseCompanyResponse.class);

      if (response == null) {
        return Optional.empty();
      }

      var addressStr = formatAddress(response.registeredOfficeAddress());

      log.info("[COMPANIES-HOUSE] Found company={} status={}", response.companyName(), response.companyStatus());

      return Optional.of(new CompanyProfile(
          response.companyName(),
          response.companyNumber(),
          "GB",
          response.companyStatus(),
          response.type(),
          response.dateOfCreation(),
          addressStr));
    } catch (HttpClientErrorException.NotFound ex) {
      log.info("[COMPANIES-HOUSE] Company not found registrationNumber={}", registrationNumber);
      return Optional.empty();
    }
  }

  @SuppressWarnings("unused")
  private Optional<CompanyProfile> lookupFallback(String registrationNumber, String country, Exception ex) {
    log.error("[COMPANIES-HOUSE] Resilience fallback — lookup failed registrationNumber={} due to {}",
        registrationNumber, ex.getClass().getSimpleName(), ex);
    throw new IllegalStateException("Companies House lookup unavailable", ex);
  }

  private String formatAddress(CompaniesHouseAddress address) {
    if (address == null) {
      return null;
    }
    var sb = new StringBuilder();
    appendIfPresent(sb, address.addressLine1());
    appendIfPresent(sb, address.addressLine2());
    appendIfPresent(sb, address.locality());
    appendIfPresent(sb, address.region());
    appendIfPresent(sb, address.postalCode());
    appendIfPresent(sb, address.country());
    return sb.toString();
  }

  private void appendIfPresent(StringBuilder sb, String value) {
    if (value != null && !value.isBlank()) {
      if (!sb.isEmpty()) {
        sb.append(", ");
      }
      sb.append(value);
    }
  }
}
