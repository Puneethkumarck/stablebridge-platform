package com.stablecoin.payments.custody.infrastructure.provider.evm;

import com.stablecoin.payments.custody.domain.model.ChainId;
import com.stablecoin.payments.custody.domain.port.TransactionReceipt;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.math.BigDecimal;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sandbox integration tests that hit real Alchemy RPC endpoints on Base Sepolia and Ethereum Sepolia.
 *
 * <p>These tests are skipped in CI unless {@code ALCHEMY_API_KEY} is set.
 * Free tier: 30M CU/month, 500 CU/s throughput.
 *
 * <p>Run manually:
 * <pre>
 *   ALCHEMY_API_KEY=your-key ./gradlew :blockchain-custody:blockchain-custody:test --tests '*EvmRpcAdapterSandboxTest*'
 * </pre>
 */
@Tag("sandbox")
@EnabledIfEnvironmentVariable(named = "ALCHEMY_API_KEY", matches = ".+")
@DisplayName("EvmRpcAdapter — Alchemy Sandbox")
class EvmRpcAdapterSandboxTest {

    private static final String BASE_SEPOLIA_USDC = "0x036CbD53842c5426634e7929541eC2318f3dCF7e";
    private static final String ETH_SEPOLIA_USDC = "0x1c7D4B196Cb0C7B01d743Fbc6116a902379C7238";
    private static final ChainId BASE_CHAIN = new ChainId("base");
    private static final ChainId ETH_CHAIN = new ChainId("ethereum");

    private static EvmRpcAdapter adapter;

    @BeforeAll
    static void setUp() {
        var apiKey = System.getenv("ALCHEMY_API_KEY");
        var properties = new EvmChainProperties(true, Map.of(
                "base", new EvmChainProperties.ChainRpcConfig(
                        "https://base-sepolia.g.alchemy.com/v2/" + apiKey,
                        84532L, BASE_SEPOLIA_USDC, 10000, 30000),
                "ethereum", new EvmChainProperties.ChainRpcConfig(
                        "https://eth-sepolia.g.alchemy.com/v2/" + apiKey,
                        11155111L, ETH_SEPOLIA_USDC, 10000, 30000)
        ));
        adapter = new EvmRpcAdapter(properties, null);
    }

    @Nested
    @DisplayName("Base Sepolia — eth_blockNumber")
    class BaseSepoliaBlockNumber {

        @Test
        @DisplayName("should return positive block number from Base Sepolia")
        void shouldReturnPositiveBlockNumber() {
            // when
            var blockNumber = adapter.getLatestBlockNumber(BASE_CHAIN);

            // then — Base Sepolia has been running since 2024, block numbers are well above zero
            assertThat(blockNumber).isGreaterThan(0L);
        }
    }

    @Nested
    @DisplayName("Ethereum Sepolia — eth_blockNumber")
    class EthSepoliaBlockNumber {

        @Test
        @DisplayName("should return positive block number from Ethereum Sepolia")
        void shouldReturnPositiveBlockNumber() {
            // when
            var blockNumber = adapter.getLatestBlockNumber(ETH_CHAIN);

            // then — Ethereum Sepolia has been running since 2022, block numbers are well above zero
            assertThat(blockNumber).isGreaterThan(0L);
        }
    }

    @Nested
    @DisplayName("Base Sepolia — eth_call (USDC balanceOf)")
    class BaseSepoliaTokenBalance {

        @Test
        @DisplayName("should return non-negative USDC balance for zero address")
        void shouldReturnNonNegativeBalance() {
            // given — the zero address (0x0000...0000) always exists on-chain
            var zeroAddress = "0x0000000000000000000000000000000000000000";

            // when
            var balance = adapter.getTokenBalance(BASE_CHAIN, zeroAddress, BASE_SEPOLIA_USDC);

            // then — balance is non-negative (zero address typically holds 0 USDC)
            assertThat(balance).isGreaterThanOrEqualTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("should return USDC balance with correct scale (6 decimals)")
        void shouldReturnBalanceWithCorrectScale() {
            // given
            var zeroAddress = "0x0000000000000000000000000000000000000000";

            // when
            var balance = adapter.getTokenBalance(BASE_CHAIN, zeroAddress, BASE_SEPOLIA_USDC);

            // then — USDC has 6 decimal places
            assertThat(balance.scale()).isEqualTo(6);
        }
    }

    @Nested
    @DisplayName("Ethereum Sepolia — eth_call (USDC balanceOf)")
    class EthSepoliaTokenBalance {

        @Test
        @DisplayName("should return non-negative USDC balance for zero address")
        void shouldReturnNonNegativeBalance() {
            // given
            var zeroAddress = "0x0000000000000000000000000000000000000000";

            // when
            var balance = adapter.getTokenBalance(ETH_CHAIN, zeroAddress, ETH_SEPOLIA_USDC);

            // then
            assertThat(balance).isGreaterThanOrEqualTo(BigDecimal.ZERO);
        }
    }

    @Nested
    @DisplayName("Base Sepolia — eth_getTransactionReceipt")
    class BaseSepoliaTransactionReceipt {

        @Test
        @DisplayName("should return null for non-existent transaction hash")
        void shouldReturnNullForNonExistentTransaction() {
            // given — a fabricated tx hash that does not exist on-chain
            var fakeTxHash = "0x0000000000000000000000000000000000000000000000000000000000000001";

            // when
            TransactionReceipt receipt = adapter.getTransactionReceipt(BASE_CHAIN, fakeTxHash);

            // then — receipt should be null since the transaction does not exist
            assertThat(receipt).isNull();
        }
    }

    @Nested
    @DisplayName("Ethereum Sepolia — eth_getTransactionReceipt")
    class EthSepoliaTransactionReceipt {

        @Test
        @DisplayName("should return null for non-existent transaction hash")
        void shouldReturnNullForNonExistentTransaction() {
            // given
            var fakeTxHash = "0x0000000000000000000000000000000000000000000000000000000000000001";

            // when
            TransactionReceipt receipt = adapter.getTransactionReceipt(ETH_CHAIN, fakeTxHash);

            // then
            assertThat(receipt).isNull();
        }
    }
}
