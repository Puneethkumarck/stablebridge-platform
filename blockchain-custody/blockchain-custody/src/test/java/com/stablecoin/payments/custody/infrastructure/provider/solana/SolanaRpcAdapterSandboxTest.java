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
 * Sandbox integration tests that hit the Solana Devnet via Alchemy RPC.
 *
 * <p>Guarded by {@code ALCHEMY_API_KEY} env var — uses the same Alchemy key
 * as the EVM sandbox tests.
 *
 * <p>Run manually:
 * <pre>
 *   ALCHEMY_API_KEY=xxx ./gradlew :blockchain-custody:blockchain-custody:test --tests '*SolanaRpcAdapterSandboxTest*'
 * </pre>
 */
@Tag("sandbox")
@EnabledIfEnvironmentVariable(named = "ALCHEMY_API_KEY", matches = ".+")
@DisplayName("SolanaRpcAdapter — Alchemy Devnet Sandbox")
class SolanaRpcAdapterSandboxTest {

    private static final String SOLANA_DEVNET_USDC_MINT = "4zMMC9srt5Ri5X14GAgXhaHii3GnPAEERYPJgZJDncDU";
    private static final ChainId SOLANA_CHAIN = new ChainId("solana");

    private static SolanaRpcAdapter adapter;

    @BeforeAll
    static void setUp() {
        var alchemyKey = System.getenv("ALCHEMY_API_KEY");
        var properties = new SolanaChainProperties(
                true,
                "https://solana-devnet.g.alchemy.com/v2/" + alchemyKey,
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
            var slot = adapter.getLatestBlockNumber(SOLANA_CHAIN);

            assertThat(slot).isGreaterThan(0L);
        }
    }

    @Nested
    @DisplayName("Solana Devnet — getTokenAccountsByOwner (USDC)")
    class SolanaDevnetTokenBalance {

        @Test
        @DisplayName("should return non-negative USDC balance for any address")
        void shouldReturnNonNegativeBalance() {
            var randomAddress = "BPFLoaderUpgradeab1e11111111111111111111111";

            var balance = adapter.getTokenBalance(SOLANA_CHAIN, randomAddress, SOLANA_DEVNET_USDC_MINT);

            assertThat(balance).isGreaterThanOrEqualTo(BigDecimal.ZERO);
        }
    }

    @Nested
    @DisplayName("Solana Devnet — getTransaction")
    class SolanaDevnetTransactionReceipt {

        @Test
        @DisplayName("should return null for non-existent transaction signature")
        void shouldReturnNullForNonExistentTransaction() {
            var fakeSignature = "5VERv8NMvzbJMEkV8xnrLkEaWRtSz9CosKDYjCJjBRnbJLgp8uirBgmQpjKhoR4tjF3ZpRzrFmBV6UjKdiSZkQU";

            TransactionReceipt receipt = adapter.getTransactionReceipt(SOLANA_CHAIN, fakeSignature);

            assertThat(receipt).isNull();
        }
    }
}
