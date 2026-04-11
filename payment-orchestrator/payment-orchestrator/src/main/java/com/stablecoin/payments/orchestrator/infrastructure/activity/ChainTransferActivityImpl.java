package com.stablecoin.payments.orchestrator.infrastructure.activity;

import com.stablecoin.payments.custody.api.TransferRequest;
import com.stablecoin.payments.custody.client.BlockchainCustodyClient;
import com.stablecoin.payments.orchestrator.domain.workflow.activity.ChainReturnRequest;
import com.stablecoin.payments.orchestrator.domain.workflow.activity.ChainTransferActivity;
import com.stablecoin.payments.orchestrator.domain.workflow.activity.ChainTransferRequest;
import com.stablecoin.payments.orchestrator.domain.workflow.activity.ChainTransferResult;
import feign.FeignException;
import io.temporal.failure.ApplicationFailure;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import static com.stablecoin.payments.orchestrator.domain.workflow.activity.ChainTransferResult.ChainTransferStatus.FAILED;
import static com.stablecoin.payments.orchestrator.domain.workflow.activity.ChainTransferResult.ChainTransferStatus.SUBMITTED;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChainTransferActivityImpl implements ChainTransferActivity {

    private final BlockchainCustodyClient blockchainCustodyClient;

    @Override
    public ChainTransferResult submitTransfer(ChainTransferRequest request) {
        log.info("Submitting chain transfer for paymentId={}, stablecoin={}, amount={}",
                request.paymentId(), request.stablecoin(), request.amount());

        var transferRequest = new TransferRequest(
                request.paymentId(),
                request.correlationId(),
                "FORWARD",
                null,
                request.stablecoin(),
                request.amount().toPlainString(),
                request.toWalletAddress(),
                request.preferredChain()
        );

        try {
            var response = blockchainCustodyClient.submitTransfer(transferRequest);
            log.info("Chain transfer submitted for paymentId={}, transferId={}, chainId={}, txHash={}",
                    request.paymentId(), response.transferId(), response.chainId(), response.txHash());
            return new ChainTransferResult(
                    response.transferId(), response.chainId(), response.txHash(),
                    SUBMITTED, null);
        } catch (FeignException.UnprocessableEntity e) {
            if (e.getMessage() != null && e.getMessage().contains("balance")) {
                throw ApplicationFailure.newNonRetryableFailure(
                        "Insufficient balance for " + request.stablecoin(),
                        "INSUFFICIENT_BALANCE");
            }
            return new ChainTransferResult(null, null, null, FAILED, e.getMessage());
        } catch (FeignException e) {
            log.error("Chain transfer failed for paymentId={}: {}",
                    request.paymentId(), e.getMessage());
            return new ChainTransferResult(null, null, null, FAILED, e.getMessage());
        }
    }

    @Override
    public void returnTransfer(ChainReturnRequest request) {
        log.info("Submitting return transfer for originalTransferId={}, paymentId={}",
                request.originalTransferId(), request.paymentId());
        try {
            var returnRequest = new TransferRequest(
                    request.paymentId(),
                    null,
                    "RETURN",
                    request.originalTransferId(),
                    request.stablecoin(),
                    request.amount().toPlainString(),
                    request.toWalletAddress(),
                    null
            );
            blockchainCustodyClient.submitTransfer(returnRequest);
            log.info("Return transfer submitted for originalTransferId={}",
                    request.originalTransferId());
        } catch (FeignException.NotFound e) {
            log.info("Original transfer {} not found — already returned or expired",
                    request.originalTransferId());
        }
    }
}
