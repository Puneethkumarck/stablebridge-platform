package com.stablecoin.payments.custody.domain.model;

import lombok.AccessLevel;
import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

@Builder(toBuilder = true, access = AccessLevel.PACKAGE)
public record Wallet(
        UUID walletId,
        ChainId chainId,
        String address,
        String addressChecksum,
        WalletTier tier,
        WalletPurpose purpose,
        String custodian,
        String vaultAccountId,
        StablecoinTicker stablecoin,
        boolean active,
        Instant createdAt,
        Instant deactivatedAt
) {

    // -- Factory Method -------------------------------------------------

    public static Wallet create(ChainId chainId, String address, String addressChecksum,
                                WalletTier tier, WalletPurpose purpose,
                                String custodian, String vaultAccountId,
                                StablecoinTicker stablecoin) {
        if (chainId == null) {
            throw new IllegalArgumentException("chainId is required");
        }
        if (address == null || address.isBlank()) {
            throw new IllegalArgumentException("address is required");
        }
        if (addressChecksum == null || addressChecksum.isBlank()) {
            throw new IllegalArgumentException("addressChecksum is required");
        }
        if (tier == null) {
            throw new IllegalArgumentException("tier is required");
        }
        if (purpose == null) {
            throw new IllegalArgumentException("purpose is required");
        }
        if (custodian == null || custodian.isBlank()) {
            throw new IllegalArgumentException("custodian is required");
        }
        if (vaultAccountId == null || vaultAccountId.isBlank()) {
            throw new IllegalArgumentException("vaultAccountId is required");
        }
        if (stablecoin == null) {
            throw new IllegalArgumentException("stablecoin is required");
        }

        var now = Instant.now();
        return Wallet.builder()
                .walletId(UUID.randomUUID())
                .chainId(chainId)
                .address(address)
                .addressChecksum(addressChecksum)
                .tier(tier)
                .purpose(purpose)
                .custodian(custodian)
                .vaultAccountId(vaultAccountId)
                .stablecoin(stablecoin)
                .active(true)
                .createdAt(now)
                .build();
    }

    // -- Domain Methods -------------------------------------------------

    public Wallet deactivate() {
        if (!active) {
            throw new IllegalStateException(
                    "Wallet %s is already deactivated".formatted(walletId));
        }
        return toBuilder()
                .active(false)
                .deactivatedAt(Instant.now())
                .build();
    }

    // -- Query Methods --------------------------------------------------

    public boolean isActive() {
        return active;
    }
}
