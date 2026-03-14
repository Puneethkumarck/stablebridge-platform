package com.stablecoin.payments.orchestrator.infrastructure.messaging;

import com.stablecoin.payments.orchestrator.domain.workflow.PaymentWorkflow;
import com.stablecoin.payments.orchestrator.domain.workflow.dto.ChainConfirmedSignal;
import com.stablecoin.payments.orchestrator.domain.workflow.dto.FiatCollectedSignal;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Kafka consumer that receives async events from S3 and S4,
 * and forwards them as Temporal signals to the payment workflow.
 * <p>
 * Events consumed:
 * <ul>
 *   <li>{@code fiat.collected} → {@link PaymentWorkflow#onFiatCollected}</li>
 *   <li>{@code chain.transfer.confirmed} → {@link PaymentWorkflow#onChainConfirmed}</li>
 * </ul>
 * <p>
 * Idempotent: sending a signal to an already-completed workflow is a no-op
 * (caught as {@link WorkflowNotFoundException}).
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.signal.consumer.enabled", havingValue = "true", matchIfMissing = true)
public class PaymentSignalConsumer {

    private static final JsonMapper JSON_MAPPER = JsonMapper.builder().build();

    private final WorkflowClient workflowClient;

    @KafkaListener(topics = "fiat.collected", groupId = "payment-orchestrator-signals")
    public void onFiatCollected(String message) {
        try {
            var node = JSON_MAPPER.readTree(message);
            var paymentId = UUID.fromString(node.get("paymentId").asText());
            var providerReference = node.has("pspReference") ? node.get("pspReference").asText() : null;
            var amount = node.has("settledAmount")
                    ? new BigDecimal(node.get("settledAmount").asText())
                    : BigDecimal.ZERO;
            var currency = node.has("currency") ? node.get("currency").asText() : null;

            log.info("Received fiat.collected for paymentId={}", paymentId);

            var signal = new FiatCollectedSignal(paymentId, providerReference, amount, currency);
            sendSignalToWorkflow(paymentId, workflow -> workflow.onFiatCollected(signal));

            log.info("Sent onFiatCollected signal for paymentId={}", paymentId);
        } catch (WorkflowNotFoundException e) {
            log.info("Workflow not found for fiat.collected event — already completed or not started: {}",
                    e.getMessage());
        } catch (Exception e) {
            log.error("Failed to process fiat.collected event: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to process fiat.collected event", e);
        }
    }

    @KafkaListener(topics = "chain.transfer.confirmed", groupId = "payment-orchestrator-signals")
    public void onChainTransferConfirmed(String message) {
        try {
            var node = JSON_MAPPER.readTree(message);
            var paymentId = UUID.fromString(node.get("paymentId").asText());
            var txHash = node.has("txHash") ? node.get("txHash").asText() : null;
            var chainId = node.has("chainId") ? node.get("chainId").asText() : null;
            var blockNumber = node.has("blockNumber") ? Long.parseLong(node.get("blockNumber").asText()) : 0L;

            log.info("Received chain.transfer.confirmed for paymentId={}", paymentId);

            var signal = new ChainConfirmedSignal(paymentId, txHash, chainId, blockNumber);
            sendSignalToWorkflow(paymentId, workflow -> workflow.onChainConfirmed(signal));

            log.info("Sent onChainConfirmed signal for paymentId={}", paymentId);
        } catch (WorkflowNotFoundException e) {
            log.info("Workflow not found for chain.transfer.confirmed — already completed or not started: {}",
                    e.getMessage());
        } catch (Exception e) {
            log.error("Failed to process chain.transfer.confirmed event: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to process chain.transfer.confirmed event", e);
        }
    }

    private void sendSignalToWorkflow(UUID paymentId, java.util.function.Consumer<PaymentWorkflow> signalSender) {
        var workflowStub = workflowClient.newWorkflowStub(
                PaymentWorkflow.class, "payment-" + paymentId);
        signalSender.accept(workflowStub);
    }
}
