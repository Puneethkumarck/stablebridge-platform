package com.stablecoin.payments.orchestrator.infrastructure.activity;

import com.stablecoin.payments.onramp.api.CollectionRequest;
import com.stablecoin.payments.onramp.api.RefundRequest;
import com.stablecoin.payments.onramp.client.FiatOnRampClient;
import com.stablecoin.payments.orchestrator.domain.workflow.activity.FiatCollectionActivity;
import com.stablecoin.payments.orchestrator.domain.workflow.activity.FiatCollectionRequest;
import com.stablecoin.payments.orchestrator.domain.workflow.activity.FiatCollectionResult;
import com.stablecoin.payments.orchestrator.domain.workflow.activity.FiatRefundRequest;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import static com.stablecoin.payments.orchestrator.domain.workflow.activity.FiatCollectionResult.FiatCollectionStatus.FAILED;
import static com.stablecoin.payments.orchestrator.domain.workflow.activity.FiatCollectionResult.FiatCollectionStatus.INITIATED;

/**
 * Temporal activity implementation that calls S3 Fiat On-Ramp via Feign.
 * <p>
 * Collection initiation is async — actual confirmation arrives via Stripe webhook
 * to S3, which publishes a Kafka event, consumed by S1 to send a workflow signal.
 * <p>
 * Compensation: refund the collected fiat via S3 refund endpoint.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FiatCollectionActivityImpl implements FiatCollectionActivity {

    private final FiatOnRampClient fiatOnRampClient;

    @Override
    public FiatCollectionResult initiateCollection(FiatCollectionRequest request) {
        log.info("Initiating fiat collection for paymentId={}, amount={} {}",
                request.paymentId(), request.amount(), request.currency());

        var collectionRequest = new CollectionRequest(
                request.paymentId(),
                request.correlationId(),
                request.amount(),
                request.currency(),
                "ACH",
                request.sourceCountry(),
                request.currency(),
                "stripe",
                "Stripe",
                "sha256:" + request.paymentId(),
                "STRIPE_DEFAULT",
                "ACH_ROUTING",
                request.sourceCountry()
        );

        try {
            var response = fiatOnRampClient.initiateCollection(collectionRequest);
            log.info("Fiat collection initiated for paymentId={}, collectionId={}, status={}",
                    request.paymentId(), response.collectionId(), response.status());
            return new FiatCollectionResult(response.collectionId(), INITIATED, null);
        } catch (FeignException.Conflict e) {
            // 409 — idempotent success
            log.info("Fiat collection already exists for paymentId={} (idempotent)",
                    request.paymentId());
            var existing = fiatOnRampClient.getCollectionByPaymentId(request.paymentId());
            return new FiatCollectionResult(existing.collectionId(), INITIATED, null);
        } catch (FeignException e) {
            log.error("Fiat collection failed for paymentId={}: {}", request.paymentId(), e.getMessage());
            return new FiatCollectionResult(null, FAILED, e.getMessage());
        }
    }

    @Override
    public void refundCollection(FiatRefundRequest request) {
        log.info("Refunding fiat collection collectionId={} for paymentId={}, amount={} {}",
                request.collectionId(), request.paymentId(),
                request.refundAmount(), request.currency());
        try {
            var refundRequest = new RefundRequest(
                    request.refundAmount(), request.currency(), request.reason());
            fiatOnRampClient.initiateRefund(request.collectionId(), refundRequest);
            log.info("Fiat refund initiated for collectionId={}", request.collectionId());
        } catch (FeignException.NotFound e) {
            // Idempotent — collection already refunded or not found
            log.info("Collection {} already refunded or not found", request.collectionId());
        }
    }
}
