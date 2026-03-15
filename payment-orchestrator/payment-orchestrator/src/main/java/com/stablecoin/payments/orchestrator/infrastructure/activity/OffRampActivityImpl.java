package com.stablecoin.payments.orchestrator.infrastructure.activity;

import com.stablecoin.payments.offramp.api.PayoutRequest;
import com.stablecoin.payments.offramp.client.FiatOffRampClient;
import com.stablecoin.payments.orchestrator.domain.workflow.activity.OffRampActivity;
import com.stablecoin.payments.orchestrator.domain.workflow.activity.OffRampRequest;
import com.stablecoin.payments.orchestrator.domain.workflow.activity.OffRampResult;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import static com.stablecoin.payments.orchestrator.domain.workflow.activity.OffRampResult.OffRampStatus.FAILED;
import static com.stablecoin.payments.orchestrator.domain.workflow.activity.OffRampResult.OffRampStatus.INITIATED;

/**
 * Temporal activity implementation that calls S5 Fiat Off-Ramp via Feign.
 * <p>
 * Payout initiation is async — actual settlement arrives via partner webhook
 * to S5, which publishes a Kafka event consumed by S7 for ledger reconciliation.
 * <p>
 * No compensation method — once stablecoin is redeemed, the off-ramp partner
 * handles the fiat payout settlement.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OffRampActivityImpl implements OffRampActivity {

    private final FiatOffRampClient fiatOffRampClient;

    @Override
    public OffRampResult initiatePayout(OffRampRequest request) {
        log.info("Initiating off-ramp payout for paymentId={}, amount={} {}",
                request.paymentId(), request.redeemedAmount(), request.targetCurrency());

        var payoutRequest = new PayoutRequest(
                request.paymentId(),
                request.correlationId(),
                request.transferId(),
                "FIAT",
                request.stablecoin(),
                request.redeemedAmount(),
                request.targetCurrency(),
                request.appliedFxRate(),
                request.recipientId(),
                "sha256:" + request.recipientId(),
                "SEPA",
                "modulr-default",
                "Modulr",
                null, null, null, null,
                null, null, null
        );

        try {
            var response = fiatOffRampClient.initiatePayout(payoutRequest);
            log.info("Off-ramp payout initiated for paymentId={}, payoutId={}, status={}",
                    request.paymentId(), response.payoutId(), response.status());
            return new OffRampResult(response.payoutId(), INITIATED, null);
        } catch (FeignException.Conflict e) {
            // 409 — idempotent success
            log.info("Payout already exists for paymentId={} (idempotent)",
                    request.paymentId());
            var existing = fiatOffRampClient.getPayoutByPaymentId(request.paymentId());
            return new OffRampResult(existing.payoutId(), INITIATED, null);
        } catch (FeignException.UnprocessableEntity e) {
            log.error("Payout rejected for paymentId={}: {}",
                    request.paymentId(), e.getMessage());
            return new OffRampResult(null, FAILED, e.getMessage());
        } catch (FeignException e) {
            log.error("Off-ramp payout failed for paymentId={}: {}",
                    request.paymentId(), e.getMessage());
            return new OffRampResult(null, FAILED, e.getMessage());
        }
    }
}
