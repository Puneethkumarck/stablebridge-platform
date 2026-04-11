package com.stablecoin.payments.onramp.domain.model;

import lombok.AccessLevel;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static com.stablecoin.payments.onramp.domain.model.ReconciliationStatus.DISCREPANCY;
import static com.stablecoin.payments.onramp.domain.model.ReconciliationStatus.MATCHED;

@Builder(toBuilder = true, access = AccessLevel.PACKAGE)
public record ReconciliationRecord(
        UUID reconciliationId,
        UUID collectionId,
        String psp,
        String pspReference,
        BigDecimal expectedAmount,
        BigDecimal actualAmount,
        String currency,
        ReconciliationStatus status,
        String discrepancyType,
        BigDecimal discrepancyAmount,
        Instant reconciledAt,
        Instant createdAt
) {

    private static final BigDecimal TOLERANCE = new BigDecimal("0.01");

    public static ReconciliationRecord reconcile(UUID collectionId, String psp,
                                                  String pspReference, BigDecimal expectedAmount,
                                                  BigDecimal actualAmount, String currency) {
        if (collectionId == null) {
            throw new IllegalArgumentException("collectionId is required");
        }
        if (expectedAmount == null) {
            throw new IllegalArgumentException("expectedAmount is required");
        }

        var now = Instant.now();

        if (actualAmount == null) {
            return ReconciliationRecord.builder()
                    .reconciliationId(UUID.randomUUID())
                    .collectionId(collectionId)
                    .psp(psp)
                    .pspReference(pspReference)
                    .expectedAmount(expectedAmount)
                    .currency(currency)
                    .status(ReconciliationStatus.UNMATCHED)
                    .createdAt(now)
                    .build();
        }

        var difference = expectedAmount.subtract(actualAmount).abs();
        var isMatch = difference.compareTo(TOLERANCE) <= 0;

        if (isMatch) {
            return ReconciliationRecord.builder()
                    .reconciliationId(UUID.randomUUID())
                    .collectionId(collectionId)
                    .psp(psp)
                    .pspReference(pspReference)
                    .expectedAmount(expectedAmount)
                    .actualAmount(actualAmount)
                    .currency(currency)
                    .status(MATCHED)
                    .reconciledAt(now)
                    .createdAt(now)
                    .build();
        }

        return ReconciliationRecord.builder()
                .reconciliationId(UUID.randomUUID())
                .collectionId(collectionId)
                .psp(psp)
                .pspReference(pspReference)
                .expectedAmount(expectedAmount)
                .actualAmount(actualAmount)
                .currency(currency)
                .status(DISCREPANCY)
                .discrepancyType("AMOUNT_MISMATCH")
                .discrepancyAmount(difference)
                .reconciledAt(now)
                .createdAt(now)
                .build();
    }
}
