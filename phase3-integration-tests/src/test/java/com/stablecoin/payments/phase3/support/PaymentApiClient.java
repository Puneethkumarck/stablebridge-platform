package com.stablecoin.payments.phase3.support;

import tools.jackson.databind.json.JsonMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.UUID;

/**
 * HTTP client for S1 Payment Orchestrator REST API.
 * Uses JDK HttpClient with HTTP/1.1 forced.
 */
public final class PaymentApiClient {

    private final HttpClient httpClient;
    private final String baseUrl;
    private final JsonMapper jsonMapper;

    public PaymentApiClient(HttpClient httpClient, String baseUrl) {
        this.httpClient = httpClient;
        this.baseUrl = baseUrl;
        this.jsonMapper = JsonMapper.builder().findAndAddModules().build();
    }

    public PaymentApiClient(HttpClient httpClient) {
        this(httpClient, System.getProperty("s1.base.url", "http://localhost:8082/orchestrator"));
    }

    public HttpResponse<String> initiatePayment(UUID paymentId,
                                                  UUID senderId,
                                                  UUID recipientId,
                                                  String amount,
                                                  String currency,
                                                  String targetCurrency,
                                                  String sourceCountry,
                                                  String targetCountry,
                                                  String idempotencyKey) throws Exception {
        var requestBody = """
                {
                    "senderId": "%s",
                    "recipientId": "%s",
                    "sourceAmount": %s,
                    "sourceCurrency": "%s",
                    "targetCurrency": "%s",
                    "sourceCountry": "%s",
                    "targetCountry": "%s"
                }
                """.formatted(senderId, recipientId, amount, currency, targetCurrency,
                sourceCountry, targetCountry);

        return httpClient.send(
                HttpRequest.newBuilder()
                        .uri(URI.create(baseUrl + "/v1/payments"))
                        .header("Content-Type", "application/json")
                        .header("Idempotency-Key", idempotencyKey)
                        .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                        .build(),
                HttpResponse.BodyHandlers.ofString());
    }

    public HttpResponse<String> getPayment(String paymentId) throws Exception {
        return httpClient.send(
                HttpRequest.newBuilder()
                        .uri(URI.create(baseUrl + "/v1/payments/" + paymentId))
                        .GET()
                        .build(),
                HttpResponse.BodyHandlers.ofString());
    }

    public HttpResponse<String> cancelPayment(String paymentId, String reason) throws Exception {
        var requestBody = """
                {"reason": "%s"}
                """.formatted(reason);

        return httpClient.send(
                HttpRequest.newBuilder()
                        .uri(URI.create(baseUrl + "/v1/payments/" + paymentId + "/cancel"))
                        .header("Content-Type", "application/json")
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                        .build(),
                HttpResponse.BodyHandlers.ofString());
    }

    public JsonMapper jsonMapper() {
        return jsonMapper;
    }
}
