package com.stablecoin.payments.offramp.domain.service;

import com.stablecoin.payments.offramp.domain.event.FiatPayoutInitiatedEvent;
import com.stablecoin.payments.offramp.domain.event.StablecoinRedeemedEvent;
import com.stablecoin.payments.offramp.domain.exception.PayoutNotFoundException;
import com.stablecoin.payments.offramp.domain.model.BankAccount;
import com.stablecoin.payments.offramp.domain.model.MobileMoneyAccount;
import com.stablecoin.payments.offramp.domain.model.OffRampTransaction;
import com.stablecoin.payments.offramp.domain.model.PartnerIdentifier;
import com.stablecoin.payments.offramp.domain.model.PaymentRail;
import com.stablecoin.payments.offramp.domain.model.PayoutOrder;
import com.stablecoin.payments.offramp.domain.model.PayoutType;
import com.stablecoin.payments.offramp.domain.model.StablecoinRedemption;
import com.stablecoin.payments.offramp.domain.model.StablecoinTicker;
import com.stablecoin.payments.offramp.domain.port.OffRampTransactionRepository;
import com.stablecoin.payments.offramp.domain.port.PayoutEventPublisher;
import com.stablecoin.payments.offramp.domain.port.PayoutOrderRepository;
import com.stablecoin.payments.offramp.domain.port.PayoutPartnerGateway;
import com.stablecoin.payments.offramp.domain.port.RedemptionGateway;
import com.stablecoin.payments.offramp.domain.port.RedemptionRequest;
import com.stablecoin.payments.offramp.domain.port.StablecoinRedemptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class PayoutCommandHandler {

    private final PayoutOrderRepository payoutOrderRepository;
    private final StablecoinRedemptionRepository stablecoinRedemptionRepository;
    private final OffRampTransactionRepository offRampTransactionRepository;
    private final RedemptionGateway redemptionGateway;
    private final PayoutPartnerGateway payoutPartnerGateway;
    private final PayoutEventPublisher eventPublisher;

    public PayoutResult initiatePayout(UUID paymentId, UUID correlationId, UUID transferId,
                                       PayoutType payoutType, StablecoinTicker stablecoin,
                                       BigDecimal redeemedAmount, String targetCurrency,
                                       BigDecimal appliedFxRate, UUID recipientId,
                                       String recipientAccountHash,
                                       BankAccount bankAccount, MobileMoneyAccount mobileMoneyAccount,
                                       PaymentRail paymentRail, PartnerIdentifier offRampPartner) {
        var existing = payoutOrderRepository.findByPaymentId(paymentId);
        if (existing.isPresent()) {
            log.info("Payout order already exists for paymentId={} payoutId={} status={}",
                    paymentId, existing.get().payoutId(), existing.get().status());
            return new PayoutResult(existing.get(), false);
        }

        var order = PayoutOrder.create(paymentId, correlationId, transferId,
                payoutType, stablecoin, redeemedAmount, targetCurrency,
                appliedFxRate, recipientId, recipientAccountHash,
                bankAccount, mobileMoneyAccount, paymentRail, offRampPartner);

        if (payoutType == PayoutType.HOLD_STABLECOIN) {
            order = order.holdStablecoin().completeHold();
            order = payoutOrderRepository.save(order);
            log.info("Payout held as stablecoin payoutId={} paymentId={}",
                    order.payoutId(), paymentId);
            return new PayoutResult(order, true);
        }

        order = order.startRedemption();
        var redemptionResult = redemptionGateway.redeem(new RedemptionRequest(
                order.payoutId(), stablecoin.ticker(), redeemedAmount, order.appliedFxRate()));

        order = order.completeRedemption(redemptionResult.fiatReceived());

        order = payoutOrderRepository.save(order);

        var redemption = StablecoinRedemption.create(
                order.payoutId(), stablecoin, redeemedAmount,
                redemptionResult.fiatReceived(), redemptionResult.fiatCurrency(),
                offRampPartner.partnerName(), redemptionResult.partnerReference());
        stablecoinRedemptionRepository.save(redemption);

        eventPublisher.publish(new StablecoinRedeemedEvent(
                redemption.redemptionId(),
                order.payoutId(),
                paymentId,
                correlationId,
                stablecoin.ticker(),
                redeemedAmount,
                redemptionResult.fiatReceived(),
                redemptionResult.fiatCurrency(),
                redemptionResult.redeemedAt()));

        var payoutResult = payoutPartnerGateway.initiatePayout(
                new com.stablecoin.payments.offramp.domain.port.PayoutRequest(
                        order.payoutId(),
                        order.fiatAmount(),
                        targetCurrency,
                        bankAccount,
                        mobileMoneyAccount,
                        paymentRail,
                        offRampPartner));

        order = order.initiatePayout(payoutResult.partnerReference());

        var offRampTxn = OffRampTransaction.create(
                order.payoutId(),
                offRampPartner.partnerName(),
                "payout.initiated",
                order.fiatAmount(),
                targetCurrency,
                payoutResult.status(),
                null);
        offRampTransactionRepository.save(offRampTxn);

        order = payoutOrderRepository.save(order);

        eventPublisher.publish(new FiatPayoutInitiatedEvent(
                order.payoutId(),
                paymentId,
                correlationId,
                order.fiatAmount(),
                targetCurrency,
                paymentRail.name(),
                offRampPartner.partnerName(),
                Instant.now()));

        log.info("Payout initiated payoutId={} paymentId={} partnerRef={}",
                order.payoutId(), paymentId, payoutResult.partnerReference());

        return new PayoutResult(order, true);
    }

    public PayoutOrder getPayout(UUID payoutId) {
        return payoutOrderRepository.findById(payoutId)
                .orElseThrow(() -> new PayoutNotFoundException(payoutId));
    }

    public PayoutOrder getPayoutByPaymentId(UUID paymentId) {
        return payoutOrderRepository.findByPaymentId(paymentId)
                .orElseThrow(() -> new PayoutNotFoundException(paymentId.toString()));
    }
}
