package com.stablecoin.payments.ledger.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record AccountBalance(
        String accountCode,
        String currency,
        BigDecimal balance,
        long version,
        UUID lastEntryId,
        Instant updatedAt
) {

    public AccountBalance {
        Objects.requireNonNull(accountCode, "accountCode must not be null");
        Objects.requireNonNull(currency, "currency must not be null");
        Objects.requireNonNull(balance, "balance must not be null");
    }

    public static AccountBalance zero(String accountCode, String currency) {
        return new AccountBalance(
                accountCode,
                currency,
                BigDecimal.ZERO,
                0L,
                null,
                Instant.now()
        );
    }

    public AccountBalance applyEntry(JournalEntry entry, EntryType normalBalance) {
        Objects.requireNonNull(entry, "entry must not be null");
        Objects.requireNonNull(normalBalance, "normalBalance must not be null");
        if (!this.accountCode.equals(entry.accountCode())) {
            throw new IllegalArgumentException(
                    "Entry account code " + entry.accountCode() + " does not match balance account code " + this.accountCode);
        }
        if (!this.currency.equals(entry.currency())) {
            throw new IllegalArgumentException(
                    "Entry currency " + entry.currency() + " does not match balance currency " + this.currency);
        }

        BigDecimal newBalance;
        if (entry.entryType() == normalBalance) {
            newBalance = this.balance.add(entry.amount());
        } else {
            newBalance = this.balance.subtract(entry.amount());
        }

        return new AccountBalance(
                this.accountCode,
                this.currency,
                newBalance,
                this.version + 1,
                entry.entryId(),
                Instant.now()
        );
    }
}
