package com.stablecoin.payments.offramp.infrastructure.provider.circle;

import com.stablecoin.payments.offramp.domain.port.RedemptionRequest;
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
 * Sandbox tests that run against the real Circle sandbox API.
 * <p>
 * Requires {@code CIRCLE_SANDBOX_API_KEY} environment variable set to a valid
 * Circle sandbox API key from developers.circle.com.
 * <p>
 * Also requires {@code CIRCLE_SANDBOX_DESTINATION_ID} — a wire transfer destination
 * created in the Circle sandbox console.
 * <p>
 * These tests are excluded from CI by the {@code @Tag("sandbox")} annotation.
 * Run manually:
 * <pre>
 * CIRCLE_SANDBOX_API_KEY=xxx CIRCLE_SANDBOX_DESTINATION_ID=yyy \
 *   ./gradlew :fiat-off-ramp:fiat-off-ramp:test --tests '*CircleRedemptionAdapterSandboxTest*'
 * </pre>
 *
 * @see <a href="https://developers.circle.com/docs/test-account">Circle Sandbox</a>
 */
@Tag("sandbox")
@EnabledIfEnvironmentVariable(named = "CIRCLE_SANDBOX_API_KEY", matches = ".+")
@DisplayName("Circle Redemption Adapter Sandbox (live sandbox API)")
class CircleRedemptionAdapterSandboxTest {

    private CircleRedemptionAdapter adapter;

    @BeforeEach
    void setUp() {
        var properties = new CircleProperties(
                "https://api-sandbox.circle.com",
                System.getenv("CIRCLE_SANDBOX_API_KEY"),
                System.getenv("CIRCLE_SANDBOX_DESTINATION_ID"),
                15
        );
        adapter = new CircleRedemptionAdapter(properties, null);
    }

    private RedemptionRequest aRedemptionRequest(BigDecimal amount) {
        return new RedemptionRequest(
                UUID.randomUUID(),
                "USDC",
                amount,
                new BigDecimal("1.00")
        );
    }

    @Nested
    @DisplayName("redeem")
    class Redeem {

        @Test
        @DisplayName("should submit a USDC redemption payout to Circle sandbox and return a valid reference")
        void shouldSubmitRedemptionPayoutInSandbox() {
            var request = aRedemptionRequest(new BigDecimal("10.00"));

            var result = adapter.redeem(request);

            assertThat(result.partnerReference()).isNotBlank();
            assertThat(result.fiatReceived()).isNotNull();
            assertThat(result.fiatCurrency()).isNotBlank();
            assertThat(result.redeemedAt()).isNotNull();
        }

        @Test
        @DisplayName("should return the same payout for duplicate idempotency key (same payoutId)")
        void shouldHandleDuplicateIdempotencyKey() {
            var payoutId = UUID.randomUUID();
            var request = new RedemptionRequest(payoutId, "USDC", new BigDecimal("5.00"), new BigDecimal("1.00"));

            var firstResult = adapter.redeem(request);
            var secondResult = adapter.redeem(request);

            assertThat(secondResult.partnerReference()).isEqualTo(firstResult.partnerReference());
        }
    }
}
