package com.stablecoin.payments.phase3.support;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Simulates Modulr webhooks to S5 Fiat Off-Ramp.
 * Builds payout.completed payload with HMAC-SHA256 signature.
 */
public final class ModulrWebhookSender {

    private static final Logger log = LoggerFactory.getLogger(ModulrWebhookSender.class);

    private static final String DEFAULT_WEBHOOK_URL = "http://localhost:8087/off-ramp/internal/webhooks/partner";
    private static final String DEFAULT_WEBHOOK_SECRET = "modulr-webhook-secret-test";

    private final HttpClient httpClient;
    private final String webhookUrl;
    private final String webhookSecret;

    public ModulrWebhookSender(HttpClient httpClient, String webhookUrl, String webhookSecret) {
        this.httpClient = httpClient;
        this.webhookUrl = webhookUrl;
        this.webhookSecret = webhookSecret;
    }

    public ModulrWebhookSender(HttpClient httpClient) {
        this(httpClient, DEFAULT_WEBHOOK_URL, DEFAULT_WEBHOOK_SECRET);
    }

    /**
     * Sends a payout.completed webhook event to S5.
     *
     * @param payoutId the payout order ID
     * @param amount   the payout amount (e.g., "920.50")
     * @param currency the currency code (e.g., "EUR")
     * @return the HTTP response from S5
     */
    public HttpResponse<String> sendPayoutCompleted(String payoutId,
                                                     String amount,
                                                     String currency) throws Exception {
        var payload = buildPayoutCompletedPayload(payoutId, amount, currency);
        var timestamp = Instant.now().toString();
        var signature = computeModulrSignature(payload, timestamp);

        log.info("Sending Modulr webhook for payout={}, amount={} {}", payoutId, amount, currency);

        var response = httpClient.send(
                HttpRequest.newBuilder()
                        .uri(URI.create(webhookUrl))
                        .header("Content-Type", "application/json")
                        .header("X-Modulr-Signature", signature)
                        .header("X-Modulr-Timestamp", timestamp)
                        .POST(HttpRequest.BodyPublishers.ofString(payload))
                        .build(),
                HttpResponse.BodyHandlers.ofString());

        log.info("Modulr webhook response: status={}", response.statusCode());
        return response;
    }

    private String buildPayoutCompletedPayload(String payoutId, String amount, String currency) {
        return """
                {
                    "id": "evt_%s",
                    "type": "payout.completed",
                    "data": {
                        "payoutId": "%s",
                        "amount": "%s",
                        "currency": "%s",
                        "status": "COMPLETED",
                        "completedAt": "%s"
                    }
                }
                """.formatted(payoutId, payoutId, amount, currency, Instant.now().toString());
    }

    private String computeModulrSignature(String payload, String timestamp) throws Exception {
        var signedContent = "%s.%s".formatted(timestamp, payload);
        var mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(webhookSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        var hash = mac.doFinal(signedContent.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(hash);
    }
}
