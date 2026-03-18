package com.stablecoin.payments.merchant.onboarding.infrastructure.kyb;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.stablecoin.payments.merchant.onboarding.domain.merchant.CompanyRegistryProvider.CompanyProfile;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("CompaniesHouseAdapter")
class CompaniesHouseAdapterTest {

    private static WireMockServer wireMock;
    private CompaniesHouseAdapter adapter;

    private static final String TEST_API_KEY = "test-api-key-123";
    private static final String COMPANY_NUMBER = "00000006";

    @BeforeAll
    static void startWireMock() {
        wireMock = new WireMockServer(wireMockConfig().dynamicPort());
        wireMock.start();
    }

    @AfterAll
    static void stopWireMock() {
        wireMock.stop();
    }

    @BeforeEach
    void setUp() {
        wireMock.resetAll();
        var properties = new CompaniesHouseProperties(
                wireMock.baseUrl(), TEST_API_KEY, 5000, 10000);
        adapter = new CompaniesHouseAdapter(properties);
    }

    @Nested
    @DisplayName("successful lookup")
    class SuccessfulLookup {

        @Test
        @DisplayName("should return mapped company profile for valid company number")
        void shouldReturnMappedCompanyProfile() {
            wireMock.stubFor(get(urlEqualTo("/company/" + COMPANY_NUMBER))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody("""
                                    {
                                      "company_name": "MARINE AND GENERAL MUTUAL LIFE ASSURANCE SOCIETY",
                                      "company_number": "00000006",
                                      "company_status": "active",
                                      "type": "private-unlimited-nsc",
                                      "date_of_creation": "1862-10-25",
                                      "registered_office_address": {
                                        "address_line_1": "Cms Cameron Mckenna Llp Cannon Place",
                                        "address_line_2": "78 Cannon Street",
                                        "locality": "London",
                                        "postal_code": "EC4N 6AF",
                                        "country": "United Kingdom"
                                      },
                                      "sic_codes": ["65110"]
                                    }
                                    """)));

            var result = adapter.lookup(COMPANY_NUMBER, "GB");

            assertThat(result).isPresent();
            var expected = new CompanyProfile(
                    "MARINE AND GENERAL MUTUAL LIFE ASSURANCE SOCIETY",
                    "00000006",
                    "GB",
                    "active",
                    "private-unlimited-nsc",
                    "1862-10-25",
                    "Cms Cameron Mckenna Llp Cannon Place, 78 Cannon Street, London, EC4N 6AF, United Kingdom");
            assertThat(result.get()).usingRecursiveComparison().isEqualTo(expected);
        }

        @Test
        @DisplayName("should format address omitting null fields")
        void shouldFormatAddressOmittingNullFields() {
            wireMock.stubFor(get(urlEqualTo("/company/" + COMPANY_NUMBER))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody("""
                                    {
                                      "company_name": "TEST LTD",
                                      "company_number": "00000006",
                                      "company_status": "active",
                                      "type": "ltd",
                                      "date_of_creation": "2020-01-15",
                                      "registered_office_address": {
                                        "address_line_1": "10 High Street",
                                        "locality": "Manchester",
                                        "postal_code": "M1 1AA"
                                      }
                                    }
                                    """)));

            var result = adapter.lookup(COMPANY_NUMBER, "GB");

            assertThat(result).isPresent();
            var expected = new CompanyProfile(
                    "TEST LTD",
                    "00000006",
                    "GB",
                    "active",
                    "ltd",
                    "2020-01-15",
                    "10 High Street, Manchester, M1 1AA");
            assertThat(result.get()).usingRecursiveComparison().isEqualTo(expected);
        }
    }

    @Nested
    @DisplayName("company not found")
    class CompanyNotFound {

        @Test
        @DisplayName("should return empty when company number does not exist")
        void shouldReturnEmptyWhenCompanyNotFound() {
            wireMock.stubFor(get(urlEqualTo("/company/99999999"))
                    .willReturn(aResponse()
                            .withStatus(404)
                            .withHeader("Content-Type", "application/json")
                            .withBody("""
                                    {
                                      "errors": [{
                                        "type": "ch:service",
                                        "error": "company-profile-not-found"
                                      }]
                                    }
                                    """)));

            var result = adapter.lookup("99999999", "GB");

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("non-GB country")
    class NonGbCountry {

        @Test
        @DisplayName("should return empty for non-GB country without calling API")
        void shouldReturnEmptyForNonGbCountry() {
            var result = adapter.lookup("12345678", "US");

            assertThat(result).isEmpty();
            assertThat(wireMock.getAllServeEvents()).isEmpty();
        }
    }

    @Nested
    @DisplayName("authentication")
    class Authentication {

        @Test
        @DisplayName("should send Basic auth header with API key as username")
        void shouldSendBasicAuthHeader() {
            wireMock.stubFor(get(urlEqualTo("/company/" + COMPANY_NUMBER))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody("""
                                    {
                                      "company_name": "AUTH TEST LTD",
                                      "company_number": "00000006",
                                      "company_status": "active",
                                      "type": "ltd",
                                      "date_of_creation": "2020-01-01"
                                    }
                                    """)));

            adapter.lookup(COMPANY_NUMBER, "GB");

            // Basic auth = base64("test-api-key-123:") = "dGVzdC1hcGkta2V5LTEyMzo="
            wireMock.verify(getRequestedFor(urlEqualTo("/company/" + COMPANY_NUMBER))
                    .withHeader("Authorization", equalTo("Basic dGVzdC1hcGkta2V5LTEyMzo=")));
        }
    }

    @Nested
    @DisplayName("error handling")
    class ErrorHandling {

        @Test
        @DisplayName("should throw on server error")
        void shouldThrowOnServerError() {
            wireMock.stubFor(get(urlEqualTo("/company/" + COMPANY_NUMBER))
                    .willReturn(aResponse()
                            .withStatus(500)
                            .withHeader("Content-Type", "application/json")
                            .withBody("""
                                    {
                                      "errors": [{
                                        "type": "ch:service",
                                        "error": "internal-server-error"
                                      }]
                                    }
                                    """)));

            assertThatThrownBy(() -> adapter.lookup(COMPANY_NUMBER, "GB"))
                    .isInstanceOf(Exception.class);
        }

        @Test
        @DisplayName("should throw on read timeout")
        void shouldThrowOnReadTimeout() {
            var timeoutProperties = new CompaniesHouseProperties(
                    wireMock.baseUrl(), TEST_API_KEY, 1000, 1000);
            var timeoutAdapter = new CompaniesHouseAdapter(timeoutProperties);

            wireMock.stubFor(get(urlEqualTo("/company/" + COMPANY_NUMBER))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withFixedDelay(3000)
                            .withHeader("Content-Type", "application/json")
                            .withBody("""
                                    {
                                      "company_name": "LATE LTD",
                                      "company_number": "00000006",
                                      "company_status": "active",
                                      "type": "ltd",
                                      "date_of_creation": "2020-01-01"
                                    }
                                    """)));

            assertThatThrownBy(() -> timeoutAdapter.lookup(COMPANY_NUMBER, "GB"))
                    .isInstanceOf(Exception.class);
        }

        @Test
        @DisplayName("should return empty when API returns 401 unauthorized")
        void shouldThrowOnUnauthorized() {
            wireMock.stubFor(get(urlEqualTo("/company/" + COMPANY_NUMBER))
                    .willReturn(aResponse()
                            .withStatus(401)
                            .withHeader("Content-Type", "application/json")
                            .withBody("""
                                    {
                                      "errors": [{
                                        "type": "ch:service",
                                        "error": "invalid-authorization"
                                      }]
                                    }
                                    """)));

            assertThatThrownBy(() -> adapter.lookup(COMPANY_NUMBER, "GB"))
                    .isInstanceOf(Exception.class);
        }
    }
}
