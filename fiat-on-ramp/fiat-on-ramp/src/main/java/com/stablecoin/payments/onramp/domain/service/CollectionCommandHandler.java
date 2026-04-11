package com.stablecoin.payments.onramp.domain.service;

import com.stablecoin.payments.onramp.domain.event.CollectionFailedEvent;
import com.stablecoin.payments.onramp.domain.event.CollectionInitiatedEvent;
import com.stablecoin.payments.onramp.domain.exception.CollectionOrderNotFoundException;
import com.stablecoin.payments.onramp.domain.model.BankAccount;
import com.stablecoin.payments.onramp.domain.model.CollectionOrder;
import com.stablecoin.payments.onramp.domain.model.Money;
import com.stablecoin.payments.onramp.domain.model.PaymentRail;
import com.stablecoin.payments.onramp.domain.model.PspIdentifier;
import com.stablecoin.payments.onramp.domain.model.PspTransaction;
import com.stablecoin.payments.onramp.domain.model.PspTransactionDirection;
import com.stablecoin.payments.onramp.domain.port.CollectionEventPublisher;
import com.stablecoin.payments.onramp.domain.port.CollectionOrderRepository;
import com.stablecoin.payments.onramp.domain.port.PspGateway;
import com.stablecoin.payments.onramp.domain.port.PspPaymentRequest;
import com.stablecoin.payments.onramp.domain.port.PspTransactionRepository;
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
public class CollectionCommandHandler {

    private final CollectionOrderRepository collectionOrderRepository;
    private final PspTransactionRepository pspTransactionRepository;
    private final PspGateway pspGateway;
    private final CollectionEventPublisher eventPublisher;

    public CollectionResult initiateCollection(UUID paymentId, UUID correlationId,
                                               Money amount, PaymentRail paymentRail,
                                               PspIdentifier psp, BankAccount senderAccount) {
        var existing = collectionOrderRepository.findByPaymentId(paymentId);
        if (existing.isPresent()) {
            log.info("Collection order already exists for paymentId={} collectionId={} status={}",
                    paymentId, existing.get().collectionId(), existing.get().status());
            return new CollectionResult(existing.get(), false);
        }

        var order = CollectionOrder.initiate(paymentId, correlationId, amount, paymentRail, psp, senderAccount);

        var pspResult = pspGateway.initiatePayment(new PspPaymentRequest(
                order.collectionId(), amount, paymentRail, senderAccount, psp.pspName(),
                order.collectionId().toString()));

        order = order.initiatePayment();
        order = order.awaitConfirmation(pspResult.pspReference());

        order = collectionOrderRepository.save(order);

        var pspTransaction = PspTransaction.create(
                order.collectionId(),
                psp.pspName(),
                pspResult.pspReference(),
                PspTransactionDirection.DEBIT,
                "payment_intent.created",
                amount,
                pspResult.status(),
                null);
        pspTransactionRepository.save(pspTransaction);

        eventPublisher.publish(new CollectionInitiatedEvent(
                order.collectionId(),
                paymentId,
                correlationId,
                amount.amount(),
                amount.currency(),
                paymentRail.rail().name(),
                psp.pspName(),
                Instant.now()));

        log.info("Collection initiated collectionId={} paymentId={} pspRef={}",
                order.collectionId(), paymentId, pspResult.pspReference());

        return new CollectionResult(order, true);
    }

    public CollectionOrder getCollection(UUID collectionId) {
        return collectionOrderRepository.findById(collectionId)
                .orElseThrow(() -> new CollectionOrderNotFoundException(collectionId));
    }

    public CollectionOrder getCollectionByPaymentId(UUID paymentId) {
        return collectionOrderRepository.findByPaymentId(paymentId)
                .orElseThrow(() -> new CollectionOrderNotFoundException(paymentId));
    }

    public void expireCollection(CollectionOrder order, Instant now) {
        var expired = order.timeoutCollection("Collection expired", "OR-3001");
        collectionOrderRepository.save(expired);

        eventPublisher.publish(new CollectionFailedEvent(
                expired.collectionId(),
                expired.paymentId(),
                expired.correlationId(),
                "Collection expired",
                "OR-3001",
                now));

        log.info("Expired collection order collectionId={} paymentId={}",
                expired.collectionId(), expired.paymentId());
    }
}
