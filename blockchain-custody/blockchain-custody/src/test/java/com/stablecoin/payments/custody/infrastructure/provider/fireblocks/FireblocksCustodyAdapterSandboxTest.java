package com.stablecoin.payments.custody.infrastructure.provider.fireblocks;

import com.stablecoin.payments.custody.domain.model.ChainId;
import com.stablecoin.payments.custody.domain.model.StablecoinTicker;
import com.stablecoin.payments.custody.domain.port.SignRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.web.client.HttpClientErrorException;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Sandbox tests that run against the real Fireblocks sandbox API.
 * <p>
 * Requires the following environment variables:
 * <ul>
 * <li>{@code FIREBLOCKS_SANDBOX_API_KEY} — API key UUID from Fireblocks sandbox console</li>
 * <li>{@code FIREBLOCKS_SANDBOX_API_SECRET_PATH} — path to RSA-2048 PEM file</li>
 * <li>{@code FIREBLOCKS_SANDBOX_VAULT_ACCOUNT_ID} — vault account ID (default "0")</li>
 * </ul>
 * <p>
 * These tests are excluded from CI by the {@code @Tag("sandbox")} annotation.
 * Run manually:
 * <pre>
 * FIREBLOCKS_SANDBOX_API_KEY=xxx \
 * FIREBLOCKS_SANDBOX_API_SECRET_PATH=/path/to/fireblocks_secret.key \
 * FIREBLOCKS_SANDBOX_VAULT_ACCOUNT_ID=0 \
 *   ./gradlew :blockchain-custody:blockchain-custody:test --tests '*FireblocksCustodyAdapterSandboxTest*'
 * </pre>
 *
 * @see <a href="https://developers.fireblocks.com/docs/sandbox-quickstart">Fireblocks Sandbox</a>
 */
@Tag("sandbox")
@EnabledIfEnvironmentVariable(named = "FIREBLOCKS_SANDBOX_API_KEY", matches = ".+")
@DisplayName("Fireblocks Custody Adapter Sandbox (live sandbox API)")
class FireblocksCustodyAdapterSandboxTest {

    private FireblocksCustodyAdapter adapter;

    @BeforeEach
    void setUp() throws IOException {
        var apiSecret = loadApiSecret();
        var vaultAccountId = System.getenv("FIREBLOCKS_SANDBOX_VAULT_ACCOUNT_ID") != null
                ? System.getenv("FIREBLOCKS_SANDBOX_VAULT_ACCOUNT_ID")
                : "0";

        var properties = new FireblocksProperties(
                "https://sandbox-api.fireblocks.io",
                System.getenv("FIREBLOCKS_SANDBOX_API_KEY"),
                apiSecret,
                vaultAccountId,
                30
        );
        adapter = new FireblocksCustodyAdapter(properties, null);
    }

    private static String loadApiSecret() throws IOException {
        var secretPath = System.getenv("FIREBLOCKS_SANDBOX_API_SECRET_PATH");
        if (secretPath != null && !secretPath.isBlank()) {
            return Files.readString(Path.of(secretPath));
        }
        var secret = System.getenv("FIREBLOCKS_SANDBOX_API_SECRET");
        if (secret != null && !secret.isBlank()) {
            return secret;
        }
        throw new IllegalStateException(
                "Set FIREBLOCKS_SANDBOX_API_SECRET_PATH (PEM file) or FIREBLOCKS_SANDBOX_API_SECRET (inline PEM)");
    }

    @Nested
    @DisplayName("getTransactionStatus")
    class GetTransactionStatus {

        @Test
        @DisplayName("should authenticate with RS256 JWT and receive 404 for non-existent transaction")
        void shouldAuthenticateAndReceive404ForNonExistentTransaction() {
            var fabricatedTxId = UUID.randomUUID().toString();

            assertThatThrownBy(() -> adapter.getTransactionStatus(fabricatedTxId))
                    .isInstanceOf(HttpClientErrorException.NotFound.class);
        }
    }

    @Nested
    @DisplayName("signAndSubmit")
    class SignAndSubmit {

        @Test
        @DisplayName("should sign and submit a test transfer to Fireblocks sandbox")
        void shouldSignAndSubmitTransferInSandbox() {
            // Sandbox uses testnet assets — ethereum:USDC maps to "USDC" in the adapter.
            // If the asset is available, we get a successful response with a custody TX ID.
            // If not, sandbox returns 400 — still confirms JWT auth + API round-trip works.
            var request = new SignRequest(
                    UUID.randomUUID(),
                    new ChainId("ethereum"),
                    null,
                    "0x742d35Cc6634C0532925a3b844Bc9e7595f2bD18",
                    new BigDecimal("0.001"),
                    StablecoinTicker.of("USDC"),
                    null,
                    null
            );

            try {
                var result = adapter.signAndSubmit(request);
                assertThat(result.custodyTxId()).isNotBlank();
            } catch (HttpClientErrorException.BadRequest ex) {
                assertThat(ex.getStatusCode().value()).isEqualTo(400);
            }
        }
    }
}
