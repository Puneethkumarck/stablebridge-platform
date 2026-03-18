package com.stablecoin.payments.compliance.infrastructure.provider.onfido;

import com.stablecoin.payments.compliance.domain.model.KycStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sandbox tests that run against the real Onfido EU sandbox API.
 * <p>
 * Requires {@code ONFIDO_SANDBOX_TOKEN} environment variable set to a valid
 * Onfido sandbox API token from the Onfido developer dashboard.
 * <p>
 * Redis is not used in sandbox tests — the adapter accepts a null RedisTemplate
 * and bypasses caching.
 * <p>
 * Onfido sandbox deterministic results:
 * <ul>
 * <li>{@code last_name = "Consider"} → consider result</li>
 * <li>Any other last_name → clear result</li>
 * </ul>
 * <p>
 * These tests are excluded from CI by the {@code @Tag("sandbox")} annotation.
 * Run manually:
 * <pre>
 * ONFIDO_SANDBOX_TOKEN=api_sandbox.xxx \
 *   ./gradlew :compliance-travel-rule:compliance-travel-rule:test --tests '*OnfidoKycAdapterSandboxTest*'
 * </pre>
 *
 * @see <a href="https://documentation.onfido.com/#sandbox-testing">Onfido Sandbox</a>
 */
@Tag("sandbox")
@EnabledIfEnvironmentVariable(named = "ONFIDO_SANDBOX_TOKEN", matches = "api_sandbox\\..+")
@DisplayName("Onfido KYC Adapter Sandbox (live EU sandbox API)")
class OnfidoKycAdapterSandboxTest {

    private OnfidoKycAdapter adapter;

    @BeforeEach
    void setUp() {
        var properties = new OnfidoProperties(
                "https://api.eu.onfido.com/v3.6",
                System.getenv("ONFIDO_SANDBOX_TOKEN"),
                15,
                60
        );
        // null Redis — adapter bypasses caching in sandbox mode
        adapter = new OnfidoKycAdapter(properties, null);
    }

    @Nested
    @DisplayName("verify")
    class Verify {

        @Test
        @DisplayName("should call Onfido sandbox and return a KYC result for unknown applicants")
        void shouldReturnKycResultForUnknownApplicants() {
            var senderId = UUID.randomUUID();
            var recipientId = UUID.randomUUID();

            var result = adapter.verify(senderId, recipientId);

            assertThat(result).isNotNull();
            assertThat(result.provider()).isEqualTo("onfido");
            assertThat(result.providerRef()).contains(senderId.toString());
            assertThat(result.checkedAt()).isNotNull();
        }

        @Test
        @DisplayName("should return UNVERIFIED for applicants with no checks in sandbox")
        void shouldReturnUnverifiedForApplicantsWithNoChecks() {
            var senderId = UUID.randomUUID();
            var recipientId = UUID.randomUUID();

            var result = adapter.verify(senderId, recipientId);

            assertThat(result.senderStatus()).isEqualTo(KycStatus.UNVERIFIED);
            assertThat(result.recipientStatus()).isEqualTo(KycStatus.UNVERIFIED);
        }
    }
}
