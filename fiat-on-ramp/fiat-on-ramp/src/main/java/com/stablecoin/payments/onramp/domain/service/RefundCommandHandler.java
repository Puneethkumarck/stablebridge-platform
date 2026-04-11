package com.stablecoin.payments.onramp.domain.service;

import com.stablecoin.payments.onramp.domain.event.RefundCompletedEvent;
import com.stablecoin.payments.onramp.domain.exception.CollectionOrderNotFoundException;
import com.stablecoin.payments.onramp.domain.exception.RefundAmountExceededException;
import com.stablecoin.payments.onramp.domain.exception.RefundNotAllowedException;
import com.stablecoin.payments.onramp.domain.exception.RefundNotFoundException;
import com.stablecoin.payments.onramp.domain.model.CollectionStatus;
import com.stablecoin.payments.onramp.domain.model.Money;
import com.stablecoin.payments.onramp.domain.model.Refund;
import com.stablecoin.payments.onramp.domain.model.RefundStatus;
import com.stablecoin.payments.onramp.domain.port.CollectionEventPublisher;
import com.stablecoin.payments.onramp.domain.port.CollectionOrderRepository;
import com.stablecoin.payments.onramp.domain.port.PspGateway;
import com.stablecoin.payments.onramp.domain.port.PspRefundRequest;
import com.stablecoin.payments.onramp.domain.port.RefundRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class RefundCommandHandler {

    private final CollectionOrderRepository collectionOrderRepository;
    private final RefundRepository refundRepository;
    private final PspGateway pspGateway;
    private final CollectionEventPublisher eventPublisher;

    public Refund initiateRefund(UUID collectionId, Money refundAmount, String reason) {
        var order = collectionOrderRepository.findById(collectionId)
                .orElseThrow(() -> new CollectionOrderNotFoundException(collectionId));

        var existingRefunds = refundRepository.findByCollectionId(collectionId);
        var existingActive = existingRefunds.stream()
                .filter(r -> r.status() == RefundStatus.COMPLETED
                        || r.status() == RefundStatus.PROCESSING
                        || r.status() == RefundStatus.PENDING)
                .findFirst();
        if (existingActive.isPresent()) {
            log.info("Refund already exists for collectionId={} refundId={} status={}",
                    collectionId, existingActive.get().refundId(), existingActive.get().status());
            return existingActive.get();
        }

        if (order.status() != CollectionStatus.COLLECTED) {
            throw new RefundNotAllowedException(collectionId, order.status());
        }

        if (refundAmount.amount().compareTo(order.collectedAmount().amount()) > 0) {
            throw new RefundAmountExceededException(collectionId, refundAmount, order.collectedAmount());
        }

        var refund = Refund.initiate(collectionId, order.paymentId(), refundAmount, reason)
                .startProcessing();

        var updatedOrder = order.initiateRefund()
                .startRefundProcessing();

        var pspResult = pspGateway.initiateRefund(new PspRefundRequest(
                collectionId, order.pspReference(), refundAmount,
                order.psp().pspName(), reason));

        refund = refund.complete(pspResult.pspRefundRef());

        updatedOrder = updatedOrder.completeRefund();
        collectionOrderRepository.save(updatedOrder);

        refund = refundRepository.save(refund);

        eventPublisher.publish(new RefundCompletedEvent(
                refund.refundId(),
                collectionId,
                order.paymentId(),
                refundAmount.amount(),
                refundAmount.currency(),
                pspResult.pspRefundRef(),
                Instant.now()));

        log.info("Refund completed collectionId={} refundId={} pspRef={}",
                collectionId, refund.refundId(), pspResult.pspRefundRef());

        return refund;
    }

    public Refund getRefund(UUID refundId) {
        return refundRepository.findById(refundId)
                .orElseThrow(() -> new RefundNotFoundException(refundId));
    }
}
