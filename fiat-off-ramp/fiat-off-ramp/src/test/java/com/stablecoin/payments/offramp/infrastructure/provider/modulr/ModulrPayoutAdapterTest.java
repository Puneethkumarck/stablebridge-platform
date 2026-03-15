package com.stablecoin.payments.offramp.infrastructure.provider.modulr;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.stablecoin.payments.offramp.domain.model.AccountType;
import com.stablecoin.payments.offramp.domain.model.BankAccount;
import com.stablecoin.payments.offramp.domain.model.PartnerIdentifier;
import com.stablecoin.payments.offramp.domain.model.PaymentRail;
import com.stablecoin.payments.offramp.domain.port.PayoutRequest;
import com.stablecoin.payments.offramp.domain.port.PayoutResult;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("ModulrPayoutAdapter")
class ModulrPayoutAdapterTest {

    private static WireMockServer wireMock;
    private ModulrPayoutAdapter adapter;

    private static final UUID PAYOUT_ID = UUID.fromString("887adb57-1d2e-4f3a-b5c6-d7e8f9a0b1c2");
    private static final String SOURCE_ACCOUNT_ID = "A1100ABCD1";
    private static final String API_KEY = "SANDBOX_TEST_API_KEY";

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
        var properties = new ModulrProperties(
                wireMock.baseUrl(), API_KEY, "test-secret", SOURCE_ACCOUNT_ID, 10);
        adapter = new ModulrPayoutAdapter(properties);
    }

    private PayoutRequest aPayoutRequest() {
        return new PayoutRequest(
                PAYOUT_ID,
                new BigDecimal("9500.00"),
                "EUR",
                new BankAccount("DE89370400440532013000", "DEUTDEFF", AccountType.IBAN, "DE"),
                null,
                PaymentRail.SEPA,
                new PartnerIdentifier("modulr-001", "modulr")
        );
    }

    @Nested
    @DisplayName("initiatePayout")
    class InitiatePayout {

        @Test
        @DisplayName("should return PayoutResult on successful Modulr payment creation")
        void initiatePayout_success() {
            wireMock.stubFor(post(urlEqualTo("/api-sandbox-token/payments"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody("""
                                    {
                                      "id": "P120003AQM",
                                      "status": "VALIDATED",
                                      "createdDate": "2026-03-15T10:30:00.000+0000",
                                      "externalReference": "887adb57-1d2e-4f3a-b5c6-d7e8f9a0b1c2",
                                      "approvalStatus": "NOTNEEDED",
                                      "message": "",
                                      "details": {
                                        "sourceAccountId": "A1100ABCD1",
                                        "destinationType": "IBAN",
                                        "destination": {
                                          "type": "IBAN",
                                          "iban": "DE89370400440532013000",
                                          "name": "modulr"
                                        },
                                        "amount": 9500.00,
                                        "reference": "Payout 887adb57-1d2e-4f3a-b5c6-d7e8f9a0b1c2"
                                      }
                                    }
                                    """)));

            var result = adapter.initiatePayout(aPayoutRequest());

            var expected = new PayoutResult("P120003AQM", "VALIDATED", null);
            assertThat(result).usingRecursiveComparison()
                    .isEqualTo(expected);
        }

        @Test
        @DisplayName("should send correct JSON body and auth header to Modulr")
        void initiatePayout_verifiesRequestBodyAndAuth() {
            wireMock.stubFor(post(urlEqualTo("/api-sandbox-token/payments"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody("""
                                    {
                                      "id": "P120003AQM",
                                      "status": "VALIDATED",
                                      "createdDate": "2026-03-15T10:30:00.000+0000",
                                      "externalReference": "887adb57-1d2e-4f3a-b5c6-d7e8f9a0b1c2",
                                      "approvalStatus": "NOTNEEDED",
                                      "message": "",
                                      "details": {
                                        "sourceAccountId": "A1100ABCD1",
                                        "destinationType": "IBAN",
                                        "destination": {
                                          "type": "IBAN",
                                          "iban": "DE89370400440532013000",
                                          "name": "modulr"
                                        },
                                        "amount": 9500.00,
                                        "reference": "Payout 887adb57-1d2e-4f3a-b5c6-d7e8f9a0b1c2"
                                      }
                                    }
                                    """)));

            adapter.initiatePayout(aPayoutRequest());

            wireMock.verify(postRequestedFor(urlEqualTo("/api-sandbox-token/payments"))
                    .withHeader("Authorization", equalTo("Bearer " + API_KEY))
                    .withHeader("Content-Type", equalTo("application/json"))
                    .withRequestBody(equalToJson("""
                            {
                              "sourceAccountId": "A1100ABCD1",
                              "amount": 9500.00,
                              "currency": "EUR",
                              "reference": "Payout 887adb57-1d2e-4f3a-b5c6-d7e8f9a0b1c2",
                              "externalReference": "887adb57-1d2e-4f3a-b5c6-d7e8f9a0b1c2",
                              "destination": {
                                "type": "IBAN",
                                "iban": "DE89370400440532013000",
                                "name": "modulr"
                              },
                              "permittedScheme": "SEPA_CREDIT"
                            }
                            """)));
        }

        @Test
        @DisplayName("should throw PayoutPartnerException when Modulr returns 400")
        void initiatePayout_badRequest() {
            wireMock.stubFor(post(urlEqualTo("/api-sandbox-token/payments"))
                    .willReturn(aResponse()
                            .withStatus(400)
                            .withHeader("Content-Type", "application/json")
                            .withBody("""
                                    {
                                      "field": "amount",
                                      "code": "INVALID",
                                      "message": "Amount must be positive"
                                    }
                                    """)));

            assertThatThrownBy(() -> adapter.initiatePayout(aPayoutRequest()))
                    .isInstanceOf(Exception.class);
        }

        @Test
        @DisplayName("should throw when Modulr returns 401 unauthorized")
        void initiatePayout_unauthorized() {
            wireMock.stubFor(post(urlEqualTo("/api-sandbox-token/payments"))
                    .willReturn(aResponse()
                            .withStatus(401)
                            .withHeader("Content-Type", "application/json")
                            .withBody("""
                                    {
                                      "error": "Unauthorized",
                                      "message": "Invalid API key"
                                    }
                                    """)));

            assertThatThrownBy(() -> adapter.initiatePayout(aPayoutRequest()))
                    .isInstanceOf(Exception.class);
        }

        @Test
        @DisplayName("should throw when Modulr returns 500 server error")
        void initiatePayout_serverError() {
            wireMock.stubFor(post(urlEqualTo("/api-sandbox-token/payments"))
                    .willReturn(aResponse()
                            .withStatus(500)
                            .withHeader("Content-Type", "application/json")
                            .withBody("""
                                    {
                                      "error": "Internal Server Error",
                                      "message": "Something went wrong"
                                    }
                                    """)));

            assertThatThrownBy(() -> adapter.initiatePayout(aPayoutRequest()))
                    .isInstanceOf(Exception.class);
        }

        @Test
        @DisplayName("should throw on connection timeout")
        void initiatePayout_timeout() {
            var timeoutProperties = new ModulrProperties(
                    wireMock.baseUrl(), API_KEY, "test-secret", SOURCE_ACCOUNT_ID, 1);
            var timeoutAdapter = new ModulrPayoutAdapter(timeoutProperties);

            wireMock.stubFor(post(urlEqualTo("/api-sandbox-token/payments"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withFixedDelay(3000)
                            .withHeader("Content-Type", "application/json")
                            .withBody("""
                                    {
                                      "id": "P120003AQM",
                                      "status": "VALIDATED",
                                      "createdDate": "2026-03-15T10:30:00.000+0000",
                                      "externalReference": "887adb57-1d2e-4f3a-b5c6-d7e8f9a0b1c2",
                                      "approvalStatus": "NOTNEEDED",
                                      "message": "",
                                      "details": null
                                    }
                                    """)));

            assertThatThrownBy(() -> timeoutAdapter.initiatePayout(aPayoutRequest()))
                    .isInstanceOf(Exception.class);
        }

        @Test
        @DisplayName("should handle GBP Faster Payments without permittedScheme")
        void initiatePayout_gbpFasterPayments() {
            var gbpRequest = new PayoutRequest(
                    PAYOUT_ID,
                    new BigDecimal("8500.50"),
                    "GBP",
                    new BankAccount("GB29NWBK60161331926819", "NWBKGB2L", AccountType.IBAN, "GB"),
                    null,
                    PaymentRail.FASTER_PAYMENTS,
                    new PartnerIdentifier("modulr-001", "modulr")
            );

            wireMock.stubFor(post(urlEqualTo("/api-sandbox-token/payments"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody("""
                                    {
                                      "id": "P120004BRM",
                                      "status": "VALIDATED",
                                      "createdDate": "2026-03-15T11:00:00.000+0000",
                                      "externalReference": "887adb57-1d2e-4f3a-b5c6-d7e8f9a0b1c2",
                                      "approvalStatus": "NOTNEEDED",
                                      "message": "",
                                      "details": {
                                        "sourceAccountId": "A1100ABCD1",
                                        "destinationType": "IBAN",
                                        "destination": {
                                          "type": "IBAN",
                                          "iban": "GB29NWBK60161331926819",
                                          "name": "modulr"
                                        },
                                        "amount": 8500.50,
                                        "reference": "Payout 887adb57-1d2e-4f3a-b5c6-d7e8f9a0b1c2"
                                      }
                                    }
                                    """)));

            var result = adapter.initiatePayout(gbpRequest);

            var expected = new PayoutResult("P120004BRM", "VALIDATED", null);
            assertThat(result).usingRecursiveComparison()
                    .isEqualTo(expected);
        }
    }
}
