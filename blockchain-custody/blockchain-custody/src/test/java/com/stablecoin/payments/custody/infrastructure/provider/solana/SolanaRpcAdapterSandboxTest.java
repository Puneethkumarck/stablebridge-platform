package com.stablecoin.payments.custody.infrastructure.provider.solana;

import com.stablecoin.payments.custody.domain.model.ChainId;
import com.stablecoin.payments.custody.domain.port.TransactionReceipt;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sandbox integration tests that hit the real Solana Devnet public RPC endpoint.
 *
 * <p>Guarded by {@code SOLANA_SANDBOX_ENABLED=true} env var to avoid
 * hitting public Devnet endpoints during every CI build. Solana Devnet
 * has rate limits and can be unstable.
 *
 * <p>Run manually:
 * <pre>
 *   SOLANA_SANDBOX_ENABLED=true ./gradlew :blockchain-custody:blockchain-custody:test --tests '*SolanaRpcAdapterSandboxTest*'
 * </pre>
 */
@Tag("sandbox")
@EnabledIfEnvironmentVariable(named = "SOLANA_SANDBOX_ENABLED", matches = "true")
@DisplayName("SolanaRpcAdapter — Devnet Sandbox")
class SolanaRpcAdapterSandboxTest {

    private static final String SOLANA_DEVNET_USDC_MINT = "4zMMC9srt5Ri5X14GAgXhaHii3GnPAEERYPJgZJDncDU";
    private static final ChainId SOLANA_CHAIN = new ChainId("solana");

    private static SolanaRpcAdapter adapter;

    @BeforeAll
    static void setUp() {
        var properties = new SolanaChainProperties(
                true,
                "https://api.devnet.solana.com",
                SOLANA_DEVNET_USDC_MINT,
                "confirmed",
                10000,
                30000
        );
        adapter = new SolanaRpcAdapter(properties);
    }

    @Nested
    @DisplayName("Solana Devnet — getSlot")
    class SolanaDevnetSlot {

        @Test
        @DisplayName("should return positive slot number from Solana Devnet")
        void shouldReturnPositiveSlotNumber() {
            // when
            var slot = adapter.getLatestBlockNumber(SOLANA_CHAIN);

            // then — Solana Devnet has been running for years, slot numbers are in the billions
            assertThat(slot).isGreaterThan(0L);
        }
    }

    @Nested
    @DisplayName("Solana Devnet — getTokenAccountsByOwner (USDC)")
    class SolanaDevnetTokenBalance {

        @Test
        @DisplayName("should return non-negative USDC balance for any address")
        void shouldReturnNonNegativeBalance() {
            // given — a random address unlikely to have USDC token accounts
            var randomAddress = "BPFLoaderUpgradeab1e11111111111111111111111";

            // when
            var balance = adapter.getTokenBalance(SOLANA_CHAIN, randomAddress, SOLANA_DEVNET_USDC_MINT);

            // then — balance should be non-negative (likely zero for this address)
            assertThat(balance).isGreaterThanOrEqualTo(BigDecimal.ZERO);
        }
    }

    @Nested
    @DisplayName("Solana Devnet — getTransaction")
    class SolanaDevnetTransactionReceipt {

        @Test
        @DisplayName("should return null for non-existent transaction signature")
        void shouldReturnNullForNonExistentTransaction() {
            // given — a valid base58-encoded signature that does not exist on-chain
            // Solana signatures are 64 bytes = 88 base58 characters
            var fakeSignature = "5VERv8NMvzbJMEkV8xnrLkEaWRtSz9CosKDYjCJjBRnbJLgp8uirBgmQpjKhoR4tjF3ZpRzrFmBV6UjKdiSZkQU";

            // when
            TransactionReceipt receipt = adapter.getTransactionReceipt(SOLANA_CHAIN, fakeSignature);

            // then — receipt should be null since the transaction does not exist
            assertThat(receipt).isNull();
        }
    }
}
