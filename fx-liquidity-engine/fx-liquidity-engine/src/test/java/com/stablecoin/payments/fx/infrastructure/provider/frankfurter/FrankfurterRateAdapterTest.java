package com.stablecoin.payments.fx.infrastructure.provider.frankfurter;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.stablecoin.payments.fx.domain.model.CorridorRate;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FrankfurterRateAdapterTest {

    private static WireMockServer wireMock;
    private FrankfurterRateAdapter adapter;

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
        var properties = new FrankfurterProperties(wireMock.baseUrl(), 2000);
        adapter = new FrankfurterRateAdapter(properties);
    }

    @Nested
    @DisplayName("Successful rate fetch")
    class SuccessfulFetch {

        @Test
        @DisplayName("should return rate for valid USD to EUR pair")
        void shouldReturnRateForValidCurrencyPair() {
            wireMock.stubFor(get(urlEqualTo("/latest?from=USD&to=EUR"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody("""
                                    {
                                      "amount": 1.0,
                                      "base": "USD",
                                      "date": "2026-03-17",
                                      "rates": {
                                        "EUR": 0.86723
                                      }
                                    }
                                    """)));

            var result = adapter.getRate("USD", "EUR");

            assertThat(result).isPresent();
            var expected = CorridorRate.builder()
                    .fromCurrency("USD")
                    .toCurrency("EUR")
                    .rate(new BigDecimal("0.86723"))
                    .spreadBps(30)
                    .feeBps(30)
                    .provider("frankfurter")
                    .ageMs(0)
                    .build();
            assertThat(result.get())
                    .usingRecursiveComparison()
                    .withComparatorForType(BigDecimal::compareTo, BigDecimal.class)
                    .isEqualTo(expected);

            wireMock.verify(1, getRequestedFor(urlEqualTo("/latest?from=USD&to=EUR")));
        }

        @Test
        @DisplayName("should return rate for EUR to USD pair")
        void shouldReturnRateForEurToUsd() {
            wireMock.stubFor(get(urlEqualTo("/latest?from=EUR&to=USD"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody("""
                                    {
                                      "amount": 1.0,
                                      "base": "EUR",
                                      "date": "2026-03-17",
                                      "rates": {
                                        "USD": 1.1531
                                      }
                                    }
                                    """)));

            var result = adapter.getRate("EUR", "USD");

            assertThat(result).isPresent();
            var expected = CorridorRate.builder()
                    .fromCurrency("EUR")
                    .toCurrency("USD")
                    .rate(new BigDecimal("1.1531"))
                    .spreadBps(30)
                    .feeBps(30)
                    .provider("frankfurter")
                    .ageMs(0)
                    .build();
            assertThat(result.get())
                    .usingRecursiveComparison()
                    .withComparatorForType(BigDecimal::compareTo, BigDecimal.class)
                    .isEqualTo(expected);
        }
    }

    @Nested
    @DisplayName("Empty or missing rates")
    class EmptyRates {

        @Test
        @DisplayName("should return empty when rates map is empty")
        void shouldReturnEmptyWhenRatesMapIsEmpty() {
            wireMock.stubFor(get(urlEqualTo("/latest?from=USD&to=XYZ"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody("""
                                    {
                                      "amount": 1.0,
                                      "base": "USD",
                                      "date": "2026-03-17",
                                      "rates": {}
                                    }
                                    """)));

            var result = adapter.getRate("USD", "XYZ");

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should return empty when target currency not in rates")
        void shouldReturnEmptyWhenTargetCurrencyNotInRates() {
            wireMock.stubFor(get(urlEqualTo("/latest?from=USD&to=GBP"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody("""
                                    {
                                      "amount": 1.0,
                                      "base": "USD",
                                      "date": "2026-03-17",
                                      "rates": {
                                        "EUR": 0.86723
                                      }
                                    }
                                    """)));

            var result = adapter.getRate("USD", "GBP");

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should return empty when response body is empty JSON")
        void shouldReturnEmptyWhenResponseBodyIsEmptyJson() {
            wireMock.stubFor(get(urlEqualTo("/latest?from=USD&to=EUR"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody("{}")));

            var result = adapter.getRate("USD", "EUR");

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("Error handling")
    class ErrorHandling {

        @Test
        @DisplayName("should throw on server error 500")
        void shouldThrowOnServerError() {
            wireMock.stubFor(get(urlEqualTo("/latest?from=USD&to=EUR"))
                    .willReturn(aResponse().withStatus(500)));

            assertThatThrownBy(() -> adapter.getRate("USD", "EUR"))
                    .isInstanceOf(Exception.class);
        }

        @Test
        @DisplayName("should throw on client error 404")
        void shouldThrowOnClientError() {
            wireMock.stubFor(get(urlEqualTo("/latest?from=USD&to=EUR"))
                    .willReturn(aResponse().withStatus(404)));

            assertThatThrownBy(() -> adapter.getRate("USD", "EUR"))
                    .isInstanceOf(Exception.class);
        }

        @Test
        @DisplayName("should throw on read timeout")
        void shouldThrowOnReadTimeout() {
            wireMock.stubFor(get(urlEqualTo("/latest?from=USD&to=EUR"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withFixedDelay(5000)
                            .withBody("""
                                    {
                                      "amount": 1.0,
                                      "base": "USD",
                                      "date": "2026-03-17",
                                      "rates": {
                                        "EUR": 0.86723
                                      }
                                    }
                                    """)));

            assertThatThrownBy(() -> adapter.getRate("USD", "EUR"))
                    .isInstanceOf(Exception.class);
        }
    }

    @Nested
    @DisplayName("Provider name")
    class ProviderName {

        @Test
        @DisplayName("should return frankfurter as provider name")
        void shouldReturnProviderName() {
            assertThat(adapter.providerName()).isEqualTo("frankfurter");
        }
    }
}
