package com.stablecoin.payments.merchant.onboarding.domain.merchant;

import com.stablecoin.payments.merchant.onboarding.domain.merchant.model.core.KybStatus;
import com.stablecoin.payments.merchant.onboarding.domain.merchant.model.core.MerchantStatus;
import com.stablecoin.payments.merchant.onboarding.domain.merchant.model.core.RateLimitTier;
import com.stablecoin.payments.merchant.onboarding.domain.merchant.model.core.RiskTier;
import com.stablecoin.payments.merchant.onboarding.domain.statemachine.StateMachineException;
import com.stablecoin.payments.merchant.onboarding.fixtures.MerchantFixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Merchant aggregate")
class MerchantTest {

    @Nested
    @DisplayName("createNew")
    class CreateNew {

        @Test
        @DisplayName("should set APPLIED status and NOT_STARTED KYB status")
        void shouldSetInitialState() {
            // given / when
            var merchant = MerchantFixtures.aNewMerchant();

            // then
            assertThat(merchant.getStatus()).isEqualTo(MerchantStatus.APPLIED);
            assertThat(merchant.getKybStatus()).isEqualTo(KybStatus.NOT_STARTED);
            assertThat(merchant.getRateLimitTier()).isEqualTo(RateLimitTier.STARTER);
            assertThat(merchant.getMerchantId()).isNotNull();
            assertThat(merchant.getCreatedAt()).isNotNull();
            assertThat(merchant.getAllowedScopes()).isEmpty();
        }
    }

    @Nested
    @DisplayName("startKyb")
    class StartKyb {

        @Test
        @DisplayName("should transition APPLIED -> KYB_IN_PROGRESS")
        void shouldTransitionToKybInProgress() {
            // given
            var merchant = MerchantFixtures.aNewMerchant();

            // when
            merchant.startKyb();

            // then
            assertThat(merchant.getStatus()).isEqualTo(MerchantStatus.KYB_IN_PROGRESS);
            assertThat(merchant.getKybStatus()).isEqualTo(KybStatus.IN_PROGRESS);
        }

        @Test
        @DisplayName("should throw when already in KYB_IN_PROGRESS")
        void shouldThrowOnInvalidTransition() {
            // given
            var merchant = MerchantFixtures.aNewMerchant();
            merchant.startKyb();

            // when / then
            assertThatThrownBy(merchant::startKyb)
                    .isInstanceOf(StateMachineException.class);
        }
    }

    @Nested
    @DisplayName("kybPassed")
    class KybPassed {

        @Test
        @DisplayName("should transition KYB_IN_PROGRESS -> PENDING_APPROVAL and set risk tier")
        void shouldTransitionToPendingApproval() {
            // given
            var merchant = MerchantFixtures.aNewMerchant();
            merchant.startKyb();

            // when
            merchant.kybPassed(RiskTier.LOW);

            // then
            assertThat(merchant.getStatus()).isEqualTo(MerchantStatus.PENDING_APPROVAL);
            assertThat(merchant.getKybStatus()).isEqualTo(KybStatus.PASSED);
            assertThat(merchant.getRiskTier()).isEqualTo(RiskTier.LOW);
        }
    }

    @Nested
    @DisplayName("kybFailed")
    class KybFailed {

        @Test
        @DisplayName("should transition KYB_IN_PROGRESS -> KYB_REJECTED")
        void shouldTransitionToKybRejected() {
            // given
            var merchant = MerchantFixtures.aNewMerchant();
            merchant.startKyb();

            // when
            merchant.kybFailed();

            // then
            assertThat(merchant.getStatus()).isEqualTo(MerchantStatus.KYB_REJECTED);
            assertThat(merchant.getKybStatus()).isEqualTo(KybStatus.FAILED);
        }
    }

    @Nested
    @DisplayName("kybFlaggedForManualReview")
    class KybFlagged {

        @Test
        @DisplayName("should transition KYB_IN_PROGRESS -> KYB_MANUAL_REVIEW")
        void shouldTransitionToManualReview() {
            // given
            var merchant = MerchantFixtures.aNewMerchant();
            merchant.startKyb();

            // when
            merchant.kybFlaggedForManualReview();

            // then
            assertThat(merchant.getStatus()).isEqualTo(MerchantStatus.KYB_MANUAL_REVIEW);
            assertThat(merchant.getKybStatus()).isEqualTo(KybStatus.MANUAL_REVIEW);
        }

        @Test
        @DisplayName("should pass KYB from MANUAL_REVIEW state")
        void shouldPassFromManualReview() {
            // given
            var merchant = MerchantFixtures.aNewMerchant();
            merchant.startKyb();
            merchant.kybFlaggedForManualReview();

            // when
            merchant.kybPassed(RiskTier.MEDIUM);

            // then
            assertThat(merchant.getStatus()).isEqualTo(MerchantStatus.PENDING_APPROVAL);
        }
    }

    @Nested
    @DisplayName("activate")
    class Activate {

        @Test
        @DisplayName("should transition PENDING_APPROVAL -> ACTIVE and upgrade rate limit tier")
        void shouldActivateAndUpgradeRateLimit() {
            // given
            var merchant = MerchantFixtures.aNewMerchant();
            merchant.startKyb();
            merchant.kybPassed(RiskTier.LOW);
            var scopes = List.of("payments:read", "payments:write");

            // when
            merchant.activate(MerchantFixtures.anApprover(), scopes);

            // then
            assertThat(merchant.getStatus()).isEqualTo(MerchantStatus.ACTIVE);
            assertThat(merchant.isActive()).isTrue();
            assertThat(merchant.getRateLimitTier()).isEqualTo(RateLimitTier.GROWTH);
            assertThat(merchant.getAllowedScopes()).containsExactlyInAnyOrderElementsOf(scopes);
            assertThat(merchant.getOnboardedBy()).isEqualTo(MerchantFixtures.anApprover());
            assertThat(merchant.getActivatedAt()).isNotNull();
        }
    }

    @Nested
    @DisplayName("suspend / reactivate")
    class SuspendReactivate {

        @Test
        @DisplayName("should support ACTIVE -> SUSPENDED -> ACTIVE lifecycle")
        void shouldSuspendAndReactivate() {
            // given
            var merchant = MerchantFixtures.activeMerchant();

            // when
            merchant.suspend();

            // then
            assertThat(merchant.getStatus()).isEqualTo(MerchantStatus.SUSPENDED);
            assertThat(merchant.getSuspendedAt()).isNotNull();
            assertThat(merchant.isActive()).isFalse();

            // when
            merchant.reactivate();

            // then
            assertThat(merchant.getStatus()).isEqualTo(MerchantStatus.ACTIVE);
            assertThat(merchant.getSuspendedAt()).isNull();
        }
    }

    @Nested
    @DisplayName("close")
    class Close {

        @Test
        @DisplayName("should close ACTIVE merchant and set closedAt")
        void shouldCloseFromActive() {
            // given
            var merchant = MerchantFixtures.activeMerchant();

            // when
            merchant.close();

            // then
            assertThat(merchant.getStatus()).isEqualTo(MerchantStatus.CLOSED);
            assertThat(merchant.getClosedAt()).isNotNull();
        }

        @Test
        @DisplayName("should allow SUSPENDED -> CLOSED")
        void shouldCloseFromSuspended() {
            // given
            var merchant = MerchantFixtures.suspendedMerchant();

            // when
            merchant.close();

            // then
            assertThat(merchant.getStatus()).isEqualTo(MerchantStatus.CLOSED);
        }
    }

    @Nested
    @DisplayName("upgradeRateLimitTier")
    class UpgradeRateLimitTier {

        @Test
        @DisplayName("should upgrade GROWTH -> ENTERPRISE")
        void shouldUpgradeSuccessfully() {
            // given
            var merchant = MerchantFixtures.activeMerchant();
            assertThat(merchant.getRateLimitTier()).isEqualTo(RateLimitTier.GROWTH);

            // when
            merchant.upgradeRateLimitTier(RateLimitTier.ENTERPRISE);

            // then
            assertThat(merchant.getRateLimitTier()).isEqualTo(RateLimitTier.ENTERPRISE);
        }

        @Test
        @DisplayName("should reject downgrade attempt")
        void shouldRejectDowngrade() {
            // given
            var merchant = MerchantFixtures.activeMerchant();
            merchant.upgradeRateLimitTier(RateLimitTier.ENTERPRISE);

            // when / then
            assertThatThrownBy(() -> merchant.upgradeRateLimitTier(RateLimitTier.GROWTH))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("can only upgrade");
        }
    }

    @Nested
    @DisplayName("invalid state transitions")
    class InvalidStateTransitions {

        @ParameterizedTest(name = "{0} → {1} should throw")
        @MethodSource("invalidTransitions")
        @DisplayName("should reject invalid state transition")
        void shouldRejectInvalidTransition(String status, String action, Merchant merchant, Runnable transition) {
            assertThatThrownBy(transition::run)
                    .isInstanceOf(StateMachineException.class);
        }

        static Stream<Arguments> invalidTransitions() {
            return Stream.of(
                    Arguments.of("ACTIVE", "startKyb",
                            MerchantFixtures.activeMerchant(),
                            (Runnable) () -> MerchantFixtures.activeMerchant().startKyb()),
                    Arguments.of("APPLIED", "activate",
                            MerchantFixtures.appliedMerchant(),
                            (Runnable) () -> MerchantFixtures.appliedMerchant().activate(MerchantFixtures.anApprover(), List.of())),
                    Arguments.of("APPLIED", "suspend",
                            MerchantFixtures.appliedMerchant(),
                            (Runnable) () -> MerchantFixtures.appliedMerchant().suspend()),
                    Arguments.of("APPLIED", "reactivate",
                            MerchantFixtures.appliedMerchant(),
                            (Runnable) () -> MerchantFixtures.appliedMerchant().reactivate()),
                    Arguments.of("CLOSED", "activate",
                            MerchantFixtures.closedMerchant(),
                            (Runnable) () -> MerchantFixtures.closedMerchant().activate(MerchantFixtures.anApprover(), List.of())),
                    Arguments.of("CLOSED", "suspend",
                            MerchantFixtures.closedMerchant(),
                            (Runnable) () -> MerchantFixtures.closedMerchant().suspend()),
                    Arguments.of("SUSPENDED", "startKyb",
                            MerchantFixtures.suspendedMerchant(),
                            (Runnable) () -> MerchantFixtures.suspendedMerchant().startKyb())
            );
        }
    }
}
