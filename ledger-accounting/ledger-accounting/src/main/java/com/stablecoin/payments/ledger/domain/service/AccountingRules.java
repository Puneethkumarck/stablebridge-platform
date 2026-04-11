package com.stablecoin.payments.ledger.domain.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static com.stablecoin.payments.ledger.domain.model.EntryType.CREDIT;
import static com.stablecoin.payments.ledger.domain.model.EntryType.DEBIT;

public final class AccountingRules {

    public static final String FIAT_RECEIVABLE = "1000";
    public static final String FIAT_CASH = "1001";
    public static final String STABLECOIN_INVENTORY = "1010";
    public static final String OFF_RAMP_RECEIVABLE = "1020";
    public static final String STABLECOIN_REDEEMED = "1030";
    public static final String FIAT_PAYABLE = "2000";
    public static final String CLIENT_FUNDS_HELD = "2010";
    public static final String FX_SPREAD_REVENUE = "4000";
    public static final String TRANSACTION_FEE_REVENUE = "4001";
    public static final String IN_TRANSIT_CLEARING = "9000";

    private AccountingRules() {
    }

    public static TransactionRequest paymentInitiated(
            UUID paymentId, UUID correlationId, UUID sourceEventId,
            BigDecimal amount, String currency
    ) {
        return new TransactionRequest(
                paymentId, correlationId, "payment.initiated", sourceEventId,
                "Payment initiated, receivable recognized",
                List.of(
                        new JournalEntryRequest(DEBIT, FIAT_RECEIVABLE, amount, currency),
                        new JournalEntryRequest(CREDIT, CLIENT_FUNDS_HELD, amount, currency)
                )
        );
    }

    public static TransactionRequest fiatCollected(
            UUID paymentId, UUID correlationId, UUID sourceEventId,
            BigDecimal amount, String currency
    ) {
        return new TransactionRequest(
                paymentId, correlationId, "fiat.collected", sourceEventId,
                "Sender funds collected via payment rail",
                List.of(
                        new JournalEntryRequest(DEBIT, FIAT_CASH, amount, currency),
                        new JournalEntryRequest(CREDIT, FIAT_RECEIVABLE, amount, currency)
                )
        );
    }

    public static TransactionRequest chainTransferSubmitted(
            UUID paymentId, UUID correlationId, UUID sourceEventId,
            BigDecimal amount, String stablecoin
    ) {
        return new TransactionRequest(
                paymentId, correlationId, "chain.transfer.submitted", sourceEventId,
                "Stablecoin transfer submitted on-chain",
                List.of(
                        new JournalEntryRequest(DEBIT, STABLECOIN_INVENTORY, amount, stablecoin),
                        new JournalEntryRequest(CREDIT, IN_TRANSIT_CLEARING, amount, stablecoin)
                )
        );
    }

    public static TransactionRequest chainTransferConfirmed(
            UUID paymentId, UUID correlationId, UUID sourceEventId,
            BigDecimal amount, String stablecoin
    ) {
        return new TransactionRequest(
                paymentId, correlationId, "chain.transfer.confirmed", sourceEventId,
                "On-chain transfer confirmed",
                List.of(
                        new JournalEntryRequest(DEBIT, OFF_RAMP_RECEIVABLE, amount, stablecoin),
                        new JournalEntryRequest(CREDIT, STABLECOIN_INVENTORY, amount, stablecoin)
                )
        );
    }

    public static TransactionRequest stablecoinRedeemed(
            UUID paymentId, UUID correlationId, UUID sourceEventId,
            BigDecimal amount, String stablecoin
    ) {
        return new TransactionRequest(
                paymentId, correlationId, "stablecoin.redeemed", sourceEventId,
                "Stablecoin redemption confirmed",
                List.of(
                        new JournalEntryRequest(DEBIT, STABLECOIN_REDEEMED, amount, stablecoin),
                        new JournalEntryRequest(CREDIT, OFF_RAMP_RECEIVABLE, amount, stablecoin)
                )
        );
    }

    public static TransactionRequest fiatPayoutCompleted(
            UUID paymentId, UUID correlationId, UUID sourceEventId,
            BigDecimal amount, String currency
    ) {
        return new TransactionRequest(
                paymentId, correlationId, "fiat.payout.completed", sourceEventId,
                "SEPA payout sent to recipient",
                List.of(
                        new JournalEntryRequest(DEBIT, CLIENT_FUNDS_HELD, amount, currency),
                        new JournalEntryRequest(CREDIT, FIAT_PAYABLE, amount, currency)
                )
        );
    }

    public static TransactionRequest paymentCompletedClearing(
            UUID paymentId, UUID correlationId, UUID sourceEventId,
            BigDecimal stablecoinAmount, String stablecoin
    ) {
        return new TransactionRequest(
                paymentId, correlationId, "payment.completed", sourceEventId,
                "Clearing leg closed",
                List.of(
                        new JournalEntryRequest(DEBIT, IN_TRANSIT_CLEARING, stablecoinAmount, stablecoin),
                        new JournalEntryRequest(CREDIT, STABLECOIN_REDEEMED, stablecoinAmount, stablecoin)
                )
        );
    }

    public static TransactionRequest paymentCompletedRevenue(
            UUID paymentId, UUID correlationId, UUID sourceEventId,
            BigDecimal feeAmount, String currency
    ) {
        return new TransactionRequest(
                paymentId, correlationId, "payment.completed.revenue", sourceEventId,
                "FX spread revenue recognized",
                List.of(
                        new JournalEntryRequest(DEBIT, CLIENT_FUNDS_HELD, feeAmount, currency),
                        new JournalEntryRequest(CREDIT, FX_SPREAD_REVENUE, feeAmount, currency)
                )
        );
    }

    public static List<JournalEntryRequest> reversalEntries(List<JournalEntryRequest> originalEntries) {
        return originalEntries.stream()
                .map(e -> new JournalEntryRequest(
                        e.entryType().opposite(),
                        e.accountCode(),
                        e.amount(),
                        e.currency()
                ))
                .toList();
    }
}
