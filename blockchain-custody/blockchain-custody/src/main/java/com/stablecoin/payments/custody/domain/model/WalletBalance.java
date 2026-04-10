package com.stablecoin.payments.custody.domain.model;

import lombok.AccessLevel;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Builder(toBuilder = true, access = AccessLevel.PACKAGE)
public record WalletBalance(
        UUID balanceId,
        UUID walletId,
        ChainId chainId,
        StablecoinTicker stablecoin,
        BigDecimal availableBalance,
        BigDecimal reservedBalance,
        BigDecimal blockchainBalance,
        long lastIndexedBlock,
        long version,
        Instant updatedAt
) {

    public WalletBalance {
        if (availableBalance != null && availableBalance.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Available balance must be non-negative");
        }
        if (reservedBalance != null && reservedBalance.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Reserved balance must be non-negative");
        }
        if (blockchainBalance != null && blockchainBalance.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Blockchain balance must be non-negative");
        }
    }

    // -- Factory Method -------------------------------------------------

    public static WalletBalance initialize(UUID walletId, ChainId chainId,
                                           StablecoinTicker stablecoin) {
        if (walletId == null) {
            throw new IllegalArgumentException("walletId is required");
        }
        if (chainId == null) {
            throw new IllegalArgumentException("chainId is required");
        }
        if (stablecoin == null) {
            throw new IllegalArgumentException("stablecoin is required");
        }

        return WalletBalance.builder()
                .balanceId(UUID.randomUUID())
                .walletId(walletId)
                .chainId(chainId)
                .stablecoin(stablecoin)
                .availableBalance(BigDecimal.ZERO)
                .reservedBalance(BigDecimal.ZERO)
                .blockchainBalance(BigDecimal.ZERO)
                .lastIndexedBlock(0L)
                .version(0L)
                .updatedAt(Instant.now())
                .build();
    }

    // -- Domain Methods -------------------------------------------------

    public WalletBalance reserve(BigDecimal amount) {
        validatePositiveAmount(amount);
        if (availableBalance.compareTo(amount) < 0) {
            throw new IllegalStateException(
                    "Insufficient available balance: available=%s, requested=%s"
                            .formatted(availableBalance, amount));
        }
        return toBuilder()
                .availableBalance(availableBalance.subtract(amount))
                .reservedBalance(reservedBalance.add(amount))
                .updatedAt(Instant.now())
                .build();
    }

    public WalletBalance release(BigDecimal amount) {
        validatePositiveAmount(amount);
        if (reservedBalance.compareTo(amount) < 0) {
            throw new IllegalStateException(
                    "Insufficient reserved balance: reserved=%s, requested=%s"
                            .formatted(reservedBalance, amount));
        }
        return toBuilder()
                .availableBalance(availableBalance.add(amount))
                .reservedBalance(reservedBalance.subtract(amount))
                .updatedAt(Instant.now())
                .build();
    }

    public WalletBalance confirmDebit(BigDecimal amount) {
        validatePositiveAmount(amount);
        if (reservedBalance.compareTo(amount) < 0) {
            throw new IllegalStateException(
                    "Insufficient reserved balance for debit: reserved=%s, requested=%s"
                            .formatted(reservedBalance, amount));
        }
        return toBuilder()
                .reservedBalance(reservedBalance.subtract(amount))
                .updatedAt(Instant.now())
                .build();
    }

    public WalletBalance syncFromChain(BigDecimal onChainBalance, long blockNumber) {
        if (onChainBalance == null || onChainBalance.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("On-chain balance must be non-negative");
        }
        if (blockNumber <= lastIndexedBlock) {
            throw new IllegalArgumentException(
                    "Block number %d must be greater than last indexed block %d"
                            .formatted(blockNumber, lastIndexedBlock));
        }
        var newAvailable = onChainBalance.subtract(reservedBalance);
        if (newAvailable.compareTo(BigDecimal.ZERO) < 0) {
            newAvailable = BigDecimal.ZERO;
        }
        return toBuilder()
                .blockchainBalance(onChainBalance)
                .availableBalance(newAvailable)
                .lastIndexedBlock(blockNumber)
                .updatedAt(Instant.now())
                .build();
    }

    // -- Query Methods --------------------------------------------------

    public boolean hasSufficientBalance(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return false;
        }
        return availableBalance.compareTo(amount) >= 0;
    }

    // -- Validation Helpers ---------------------------------------------

    private static void validatePositiveAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
    }
}
