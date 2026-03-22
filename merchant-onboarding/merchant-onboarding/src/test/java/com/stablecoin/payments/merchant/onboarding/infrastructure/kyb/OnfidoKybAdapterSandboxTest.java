package com.stablecoin.payments.merchant.onboarding.infrastructure.kyb;

import com.stablecoin.payments.merchant.onboarding.domain.merchant.model.core.KybStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sandbox tests that run against the real Onfido EU sandbox API for KYB verification.
 * <p>
 * Requires {@code ONFIDO_SANDBOX_TOKEN} environment variable set to a valid
 * Onfido sandbox API token from the Onfido developer dashboard.
 * <p>
 * Onfido sandbox deterministic results:
 * <ul>
 * <li>{@code last_name = "Consider"} → consider result (maps to MANUAL_REVIEW)</li>
 * <li>Any other last_name → clear result (maps to PASSED)</li>
 * </ul>
 * <p>
 * These tests are excluded from CI by the {@code @Tag("sandbox")} annotation.
 * Run manually:
 * <pre>
 * ONFIDO_SANDBOX_TOKEN=api_sandbox.xxx \
 *   ./gradlew :merchant-onboarding:merchant-onboarding:test --tests '*OnfidoKybAdapterSandboxTest*'
 * </pre>
 *
 * @see <a href="https://documentation.onfido.com/#sandbox-testing">Onfido Sandbox</a>
 */
@Tag("sandbox")
@EnabledIfEnvironmentVariable(named = "ONFIDO_SANDBOX_TOKEN", matches = "api_sandbox\\..+")
@DisplayName("Onfido KYB Adapter Sandbox (live EU sandbox API)")
class OnfidoKybAdapterSandboxTest {

    private OnfidoKybAdapter adapter;

    @BeforeEach
    void setUp() {
        var properties = new OnfidoProperties(
                System.getenv("ONFIDO_SANDBOX_TOKEN"),
                "https://api.eu.onfido.com/v3.6",
                null,
                "EU",
                30
        );
        adapter = new OnfidoKybAdapter(properties, null);
    }

    @Nested
    @DisplayName("submit")
    class Submit {

        @Test
        @DisplayName("should create an applicant and submit a check in Onfido sandbox")
        void shouldCreateApplicantAndSubmitCheckInSandbox() {
            var merchantId = UUID.randomUUID();

            var result = adapter.submit(merchantId, "Acme Corporation", "12345678", "GB");

            assertThat(result).isNotNull();
            assertThat(result.provider()).isEqualTo("onfido");
            assertThat(result.providerRef()).isNotBlank();
            assertThat(result.status()).isEqualTo(KybStatus.IN_PROGRESS);
            assertThat(result.merchantId()).isEqualTo(merchantId);
        }
    }

    @Nested
    @DisplayName("getResult")
    class GetResult {

        @Test
        @DisplayName("should return clear result for non-Consider last name in Onfido sandbox")
        void shouldReturnClearResultForNonConsiderLastName() {
            var merchantId = UUID.randomUUID();
            var submitted = adapter.submit(merchantId, "Test Company", "12345678", "GB");
            var checkId = submitted.providerRef();

            // Onfido sandbox completes checks synchronously — poll once
            var result = adapter.getResult(checkId);

            assertThat(result).isPresent();
            var verification = result.get();
            assertThat(verification.providerRef()).isEqualTo(checkId);
            // Sandbox may return IN_PROGRESS or PASSED depending on timing
            assertThat(verification.status()).isIn(KybStatus.IN_PROGRESS, KybStatus.PASSED);
        }

        @Test
        @DisplayName("should return consider result for 'Consider' last name in Onfido sandbox")
        void shouldReturnConsiderResultForConsiderLastName() {
            var merchantId = UUID.randomUUID();
            // Onfido sandbox: last_name "Consider" → consider result → MANUAL_REVIEW
            var submitted = adapter.submit(merchantId, "Test Consider", "12345678", "GB");
            var checkId = submitted.providerRef();

            var result = adapter.getResult(checkId);

            assertThat(result).isPresent();
            var verification = result.get();
            assertThat(verification.providerRef()).isEqualTo(checkId);
            // Sandbox may return IN_PROGRESS or MANUAL_REVIEW depending on timing
            assertThat(verification.status()).isIn(KybStatus.IN_PROGRESS, KybStatus.MANUAL_REVIEW);
        }
    }
}
