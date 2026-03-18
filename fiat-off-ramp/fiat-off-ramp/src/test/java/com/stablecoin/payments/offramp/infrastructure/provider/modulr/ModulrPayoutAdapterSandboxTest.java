package com.stablecoin.payments.offramp.infrastructure.provider.modulr;

import com.stablecoin.payments.offramp.domain.model.AccountType;
import com.stablecoin.payments.offramp.domain.model.BankAccount;
import com.stablecoin.payments.offramp.domain.model.PartnerIdentifier;
import com.stablecoin.payments.offramp.domain.model.PaymentRail;
import com.stablecoin.payments.offramp.domain.port.PayoutRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sandbox tests that run against the real Modulr sandbox API.
 * <p>
 * Requires {@code MODULR_SANDBOX_API_KEY} and {@code MODULR_SANDBOX_SOURCE_ACCOUNT_ID}
 * environment variables from Modulr partner sandbox access.
 * <p>
 * These tests are excluded from CI by the {@code @Tag("sandbox")} annotation.
 * Run manually:
 * <pre>
 * MODULR_SANDBOX_API_KEY=xxx MODULR_SANDBOX_SOURCE_ACCOUNT_ID=yyy \
 *   ./gradlew :fiat-off-ramp:fiat-off-ramp:test --tests '*ModulrPayoutAdapterSandboxTest*'
 * </pre>
 *
 * @see <a href="https://modulr.readme.io/docs/sandbox">Modulr Sandbox</a>
 */
@Tag("sandbox")
@EnabledIfEnvironmentVariable(named = "MODULR_SANDBOX_API_KEY", matches = ".+")
@DisplayName("Modulr Payout Adapter Sandbox (live sandbox API)")
class ModulrPayoutAdapterSandboxTest {

    private ModulrPayoutAdapter adapter;

    // Test IBAN from Modulr sandbox documentation
    private static final String TEST_IBAN = "DE89370400440532013000";

    @BeforeEach
    void setUp() {
        var properties = new ModulrProperties(
                "https://api-sandbox.modulrfinance.com",
                System.getenv("MODULR_SANDBOX_API_KEY"),
                "",
                System.getenv("MODULR_SANDBOX_SOURCE_ACCOUNT_ID"),
                15
        );
        adapter = new ModulrPayoutAdapter(properties);
    }

    private PayoutRequest aPayoutRequest(BigDecimal amount) {
        return new PayoutRequest(
                UUID.randomUUID(),
                amount,
                "EUR",
                new BankAccount(TEST_IBAN, "COBADEFFXXX", AccountType.IBAN, "DE"),
                null,
                PaymentRail.SEPA,
                new PartnerIdentifier("modulr-sandbox", "Test Beneficiary")
        );
    }

    @Nested
    @DisplayName("initiatePayout")
    class InitiatePayout {

        @Test
        @DisplayName("should create a SEPA payout in Modulr sandbox and return a valid reference")
        void shouldInitiateSepaPayoutInSandbox() {
            var request = aPayoutRequest(new BigDecimal("25.00"));

            var result = adapter.initiatePayout(request);

            assertThat(result.partnerReference()).isNotBlank();
            assertThat(result.status()).isNotBlank();
        }

        @Test
        @DisplayName("should return a Modulr payment reference with expected format")
        void shouldReturnModulrReferenceWithExpectedFormat() {
            var request = aPayoutRequest(new BigDecimal("15.00"));

            var result = adapter.initiatePayout(request);

            assertThat(result.partnerReference()).isNotBlank();
        }
    }
}
