package com.stablecoin.payments.phase3.support;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Simulates Stripe webhooks to S3 Fiat On-Ramp.
 * Builds charge.succeeded payload with HMAC-SHA256 signature.
 */
public final class StripeWebhookSender {

    private static final Logger log = LoggerFactory.getLogger(StripeWebhookSender.class);

    private static final String DEFAULT_WEBHOOK_URL = "http://localhost:8085/on-ramp/internal/webhooks/psp/stripe";
    private static final String DEFAULT_WEBHOOK_SECRET = "whsec_test_default";

    private final HttpClient httpClient;
    private final String webhookUrl;
    private final String webhookSecret;

    public StripeWebhookSender(HttpClient httpClient, String webhookUrl, String webhookSecret) {
        this.httpClient = httpClient;
        this.webhookUrl = webhookUrl;
        this.webhookSecret = webhookSecret;
    }

    public StripeWebhookSender(HttpClient httpClient) {
        this(httpClient, DEFAULT_WEBHOOK_URL, DEFAULT_WEBHOOK_SECRET);
    }

    /**
     * Sends a charge.succeeded webhook event to S3.
     *
     * @param collectionId the collection order ID (used as metadata reference)
     * @param amount       the collected amount in minor units (e.g., 100000 for $1000.00)
     * @param currency     the currency code (e.g., "usd")
     * @return the HTTP response from S3
     */
    public HttpResponse<String> sendCollectionSuccess(String collectionId,
                                                       long amount,
                                                       String currency) throws Exception {
        var payload = buildChargeSucceededPayload(collectionId, amount, currency);
        var timestamp = Instant.now().getEpochSecond();
        var signature = computeStripeSignature(payload, timestamp);

        var signatureHeader = "t=%d,v1=%s".formatted(timestamp, signature);

        log.info("Sending Stripe webhook for collection={}, amount={} {}", collectionId, amount, currency);

        var response = httpClient.send(
                HttpRequest.newBuilder()
                        .uri(URI.create(webhookUrl))
                        .header("Content-Type", "application/json")
                        .header("Stripe-Signature", signatureHeader)
                        .POST(HttpRequest.BodyPublishers.ofString(payload))
                        .build(),
                HttpResponse.BodyHandlers.ofString());

        log.info("Stripe webhook response: status={}", response.statusCode());
        return response;
    }

    private String buildChargeSucceededPayload(String collectionId, long amount, String currency) {
        return """
                {
                    "id": "evt_%s",
                    "object": "event",
                    "type": "charge.succeeded",
                    "data": {
                        "object": {
                            "id": "%s",
                            "object": "charge",
                            "amount": %d,
                            "currency": "%s",
                            "status": "succeeded",
                            "metadata": {
                                "collectionId": "%s"
                            }
                        }
                    }
                }
                """.formatted(collectionId, collectionId, amount, currency, collectionId);
    }

    private String computeStripeSignature(String payload, long timestamp) throws Exception {
        var signedContent = "%d.%s".formatted(timestamp, payload);
        var mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(webhookSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        var hash = mac.doFinal(signedContent.getBytes(StandardCharsets.UTF_8));
        return bytesToHex(hash);
    }

    private static String bytesToHex(byte[] bytes) {
        var sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
