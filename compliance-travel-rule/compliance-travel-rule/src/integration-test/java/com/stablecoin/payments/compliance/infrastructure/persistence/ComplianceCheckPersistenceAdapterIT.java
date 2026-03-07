package com.stablecoin.payments.compliance.infrastructure.persistence;

import com.stablecoin.payments.compliance.AbstractIntegrationTest;
import com.stablecoin.payments.compliance.domain.model.AmlResult;
import com.stablecoin.payments.compliance.domain.model.ComplianceCheck;
import com.stablecoin.payments.compliance.domain.model.ComplianceCheckStatus;
import com.stablecoin.payments.compliance.domain.model.KycResult;
import com.stablecoin.payments.compliance.domain.model.KycStatus;
import com.stablecoin.payments.compliance.domain.model.KycTier;
import com.stablecoin.payments.compliance.domain.model.Money;
import com.stablecoin.payments.compliance.domain.model.OverallResult;
import com.stablecoin.payments.compliance.domain.model.RiskBand;
import com.stablecoin.payments.compliance.domain.model.RiskScore;
import com.stablecoin.payments.compliance.domain.model.SanctionsResult;
import com.stablecoin.payments.compliance.domain.model.TransmissionStatus;
import com.stablecoin.payments.compliance.domain.model.TravelRulePackage;
import com.stablecoin.payments.compliance.domain.model.TravelRuleProtocol;
import com.stablecoin.payments.compliance.domain.model.VaspInfo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("ComplianceCheckPersistenceAdapter IT")
class ComplianceCheckPersistenceAdapterIT extends AbstractIntegrationTest {

    @Autowired
    private ComplianceCheckPersistenceAdapter adapter;

    private static final Money SOURCE_AMOUNT = new Money(new BigDecimal("1000.00"), "USD");

    // ── Basic CRUD ──────────────────────────────────────────────────────

    @Test
    @DisplayName("should save and find pending check by id")
    void shouldSaveAndFindPendingCheckById() {
        var check = createPendingCheck();
        var saved = adapter.save(check);

        var found = adapter.findById(saved.checkId());

        assertThat(found).isPresent();
        assertThat(found.get().checkId()).isEqualTo(check.checkId());
        assertThat(found.get().paymentId()).isEqualTo(check.paymentId());
        assertThat(found.get().senderId()).isEqualTo(check.senderId());
        assertThat(found.get().recipientId()).isEqualTo(check.recipientId());
        assertThat(found.get().status()).isEqualTo(ComplianceCheckStatus.PENDING);
        assertThat(found.get().sourceAmount()).isEqualByComparingTo(new BigDecimal("1000.00"));
        assertThat(found.get().sourceCurrency()).isEqualTo("USD");
        assertThat(found.get().targetCurrency()).isEqualTo("EUR");
        assertThat(found.get().sourceCountry()).isEqualTo("US");
        assertThat(found.get().targetCountry()).isEqualTo("DE");
        assertThat(found.get().overallResult()).isNull();
        assertThat(found.get().kycResult()).isNull();
        assertThat(found.get().sanctionsResult()).isNull();
        assertThat(found.get().amlResult()).isNull();
        assertThat(found.get().travelRulePackage()).isNull();
    }

    @Test
    @DisplayName("should find check by payment id")
    void shouldFindByPaymentId() {
        var check = createPendingCheck();
        adapter.save(check);

        var found = adapter.findByPaymentId(check.paymentId());

        assertThat(found).isPresent();
        assertThat(found.get().checkId()).isEqualTo(check.checkId());
    }

    @Test
    @DisplayName("should return empty when id not found")
    void shouldReturnEmptyWhenIdNotFound() {
        assertThat(adapter.findById(UUID.randomUUID())).isEmpty();
    }

    @Test
    @DisplayName("should return empty when payment id not found")
    void shouldReturnEmptyWhenPaymentIdNotFound() {
        assertThat(adapter.findByPaymentId(UUID.randomUUID())).isEmpty();
    }

    // ── Sub-entity persistence ──────────────────────────────────────────

    @Test
    @DisplayName("should save check with KYC result and verify JSONB raw response default")
    void shouldSaveCheckWithKycResult() {
        var check = createPendingCheck();
        check = adapter.save(check);
        check = check.startKyc();
        var kycResult = KycResult.builder()
                .kycResultId(UUID.randomUUID())
                .checkId(check.checkId())
                .senderKycTier(KycTier.KYC_TIER_2)
                .senderStatus(KycStatus.VERIFIED)
                .recipientStatus(KycStatus.VERIFIED)
                .provider("onfido")
                .providerRef("ref-kyc-123")
                .checkedAt(Instant.now())
                .build();
        check = check.passKyc(kycResult);
        adapter.save(check);

        var found = adapter.findById(check.checkId()).orElseThrow();

        assertThat(found.status()).isEqualTo(ComplianceCheckStatus.SANCTIONS_SCREENING);
        assertThat(found.kycResult()).isNotNull();
        assertThat(found.kycResult().kycResultId()).isEqualTo(kycResult.kycResultId());
        assertThat(found.kycResult().senderKycTier()).isEqualTo(KycTier.KYC_TIER_2);
        assertThat(found.kycResult().senderStatus()).isEqualTo(KycStatus.VERIFIED);
        assertThat(found.kycResult().recipientStatus()).isEqualTo(KycStatus.VERIFIED);
        assertThat(found.kycResult().provider()).isEqualTo("onfido");
    }

    @Test
    @DisplayName("should save sanctions result with JSONB hit details and text[] lists")
    void shouldSaveSanctionsResultWithJsonbAndTextArray() {
        var check = progressToSanctionsScreening();
        var sanctionsResult = SanctionsResult.builder()
                .sanctionsResultId(UUID.randomUUID())
                .checkId(check.checkId())
                .senderScreened(true)
                .recipientScreened(true)
                .senderHit(true)
                .recipientHit(false)
                .hitDetails("{\"list\":\"OFAC\",\"matchScore\":0.95}")
                .listsChecked(List.of("OFAC", "EU", "UN"))
                .provider("chainalysis")
                .providerRef("ref-sanctions-456")
                .screenedAt(Instant.now())
                .build();
        check = check.sanctionsHitDetected(sanctionsResult);
        adapter.save(check);

        var found = adapter.findById(check.checkId()).orElseThrow();

        assertThat(found.status()).isEqualTo(ComplianceCheckStatus.SANCTIONS_HIT);
        assertThat(found.sanctionsResult()).isNotNull();
        assertThat(found.sanctionsResult().senderHit()).isTrue();
        assertThat(found.sanctionsResult().recipientHit()).isFalse();
        assertThat(found.sanctionsResult().hitDetails()).contains("OFAC").contains("0.95");
        assertThat(found.sanctionsResult().listsChecked()).containsExactly("OFAC", "EU", "UN");
    }

    @Test
    @DisplayName("should save AML result with JSONB chain analysis and text[] flag reasons")
    void shouldSaveAmlResultWithJsonbAndTextArray() {
        var check = progressToAmlScreening();
        var amlResult = AmlResult.builder()
                .amlResultId(UUID.randomUUID())
                .checkId(check.checkId())
                .flagged(true)
                .flagReasons(List.of("high_risk_jurisdiction", "unusual_pattern"))
                .chainAnalysis("{\"risk\":\"high\",\"exposure\":{\"darknet\":0.01}}")
                .provider("chainalysis")
                .providerRef("ref-aml-789")
                .screenedAt(Instant.now())
                .build();
        check = check.amlFlagged(amlResult);
        adapter.save(check);

        var found = adapter.findById(check.checkId()).orElseThrow();

        assertThat(found.status()).isEqualTo(ComplianceCheckStatus.MANUAL_REVIEW);
        assertThat(found.amlResult()).isNotNull();
        assertThat(found.amlResult().flagged()).isTrue();
        assertThat(found.amlResult().flagReasons()).containsExactly("high_risk_jurisdiction", "unusual_pattern");
        assertThat(found.amlResult().chainAnalysis()).contains("high").contains("darknet");
    }

    @Test
    @DisplayName("should save travel rule package with JSONB vasp info and BYTEA data")
    void shouldSaveTravelRulePackageWithJsonbAndBytea() {
        var check = progressToTravelRulePackaging();
        var travelRule = TravelRulePackage.builder()
                .packageId(UUID.randomUUID())
                .checkId(check.checkId())
                .originatorVasp(new VaspInfo("vasp-1", "StableBridge US", "US", "did:web:stablebridge.us"))
                .beneficiaryVasp(new VaspInfo("vasp-2", "StableBridge DE", "DE", "did:web:stablebridge.de"))
                .originatorData("{\"name\":\"John Doe\",\"account\":\"US123\"}")
                .beneficiaryData("{\"name\":\"Hans Mueller\",\"account\":\"DE456\"}")
                .protocol(TravelRuleProtocol.IVMS101)
                .transmissionStatus(TransmissionStatus.TRANSMITTED)
                .transmittedAt(Instant.now())
                .protocolRef("trisa-ref-001")
                .build();
        check = check.completeTravelRule(travelRule);
        adapter.save(check);

        var found = adapter.findById(check.checkId()).orElseThrow();

        assertThat(found.status()).isEqualTo(ComplianceCheckStatus.PASSED);
        assertThat(found.travelRulePackage()).isNotNull();
        assertThat(found.travelRulePackage().originatorVasp().vaspId()).isEqualTo("vasp-1");
        assertThat(found.travelRulePackage().originatorVasp().name()).isEqualTo("StableBridge US");
        assertThat(found.travelRulePackage().beneficiaryVasp().country()).isEqualTo("DE");
        assertThat(found.travelRulePackage().beneficiaryVasp().did()).isEqualTo("did:web:stablebridge.de");
        assertThat(found.travelRulePackage().originatorData()).isEqualTo("{\"name\":\"John Doe\",\"account\":\"US123\"}");
        assertThat(found.travelRulePackage().beneficiaryData()).isEqualTo("{\"name\":\"Hans Mueller\",\"account\":\"DE456\"}");
        assertThat(found.travelRulePackage().protocol()).isEqualTo(TravelRuleProtocol.IVMS101);
        assertThat(found.travelRulePackage().transmissionStatus()).isEqualTo(TransmissionStatus.TRANSMITTED);
    }

    // ── Full pipeline ───────────────────────────────────────────────────

    @Test
    @DisplayName("should save full compliance pipeline from PENDING to PASSED with risk factors")
    void shouldSaveFullCompliancePipeline() {
        var check = createPendingCheck();
        check = adapter.save(check);

        // KYC
        check = check.startKyc();
        check = check.passKyc(aKycResult(check.checkId()));
        check = adapter.save(check);

        // Sanctions
        check = check.sanctionsClear(aSanctionsClearResult(check.checkId()));
        check = adapter.save(check);

        // AML
        check = check.amlClear(anAmlClearResult(check.checkId()));
        check = adapter.save(check);

        // Risk scoring with factors (verifies text[] round-trip)
        check = check.riskScored(new RiskScore(25, RiskBand.LOW, List.of("low_amount", "known_corridor")));
        check = adapter.save(check);

        // Travel rule
        check = check.completeTravelRule(aTravelRulePackage(check.checkId()));
        check = adapter.save(check);

        var found = adapter.findById(check.checkId()).orElseThrow();

        assertThat(found.status()).isEqualTo(ComplianceCheckStatus.PASSED);
        assertThat(found.overallResult()).isEqualTo(OverallResult.PASSED);
        assertThat(found.completedAt()).isNotNull();
        assertThat(found.kycResult()).isNotNull();
        assertThat(found.sanctionsResult()).isNotNull();
        assertThat(found.amlResult()).isNotNull();
        assertThat(found.travelRulePackage()).isNotNull();
        assertThat(found.riskScore()).isNotNull();
        assertThat(found.riskScore().score()).isEqualTo(25);
        assertThat(found.riskScore().band()).isEqualTo(RiskBand.LOW);
        assertThat(found.riskScore().factors()).containsExactly("low_amount", "known_corridor");
    }

    // ── Update & constraints ────────────────────────────────────────────

    @Test
    @DisplayName("should update existing check status via adapter upsert path")
    void shouldUpdateExistingCheckStatus() {
        var check = createPendingCheck();
        check = adapter.save(check);

        check = check.startKyc();
        adapter.save(check);

        var found = adapter.findById(check.checkId()).orElseThrow();

        assertThat(found.status()).isEqualTo(ComplianceCheckStatus.KYC_IN_PROGRESS);
    }

    @Test
    @DisplayName("should enforce unique constraint on payment_id")
    void shouldEnforceUniquePaymentIdConstraint() {
        var paymentId = UUID.randomUUID();
        var check1 = ComplianceCheck.initiate(
                paymentId, UUID.randomUUID(), UUID.randomUUID(),
                SOURCE_AMOUNT, "US", "DE", "EUR");
        adapter.save(check1);

        var check2 = ComplianceCheck.initiate(
                paymentId, UUID.randomUUID(), UUID.randomUUID(),
                SOURCE_AMOUNT, "US", "DE", "EUR");

        assertThatThrownBy(() -> adapter.save(check2))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    // ── Helpers ─────────────────────────────────────────────────────────

    private static ComplianceCheck createPendingCheck() {
        return ComplianceCheck.initiate(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                SOURCE_AMOUNT, "US", "DE", "EUR");
    }

    private ComplianceCheck progressToSanctionsScreening() {
        var check = createPendingCheck();
        check = adapter.save(check);
        check = check.startKyc();
        check = check.passKyc(aKycResult(check.checkId()));
        return adapter.save(check);
    }

    private ComplianceCheck progressToAmlScreening() {
        var check = progressToSanctionsScreening();
        check = check.sanctionsClear(aSanctionsClearResult(check.checkId()));
        return adapter.save(check);
    }

    private ComplianceCheck progressToTravelRulePackaging() {
        var check = progressToAmlScreening();
        check = check.amlClear(anAmlClearResult(check.checkId()));
        check = adapter.save(check);
        check = check.riskScored(new RiskScore(25, RiskBand.LOW, List.of("low_amount")));
        return adapter.save(check);
    }

    private static KycResult aKycResult(UUID checkId) {
        return KycResult.builder()
                .kycResultId(UUID.randomUUID())
                .checkId(checkId)
                .senderKycTier(KycTier.KYC_TIER_2)
                .senderStatus(KycStatus.VERIFIED)
                .recipientStatus(KycStatus.VERIFIED)
                .provider("onfido")
                .providerRef("ref-kyc-" + UUID.randomUUID())
                .checkedAt(Instant.now())
                .build();
    }

    private static SanctionsResult aSanctionsClearResult(UUID checkId) {
        return SanctionsResult.builder()
                .sanctionsResultId(UUID.randomUUID())
                .checkId(checkId)
                .senderScreened(true)
                .recipientScreened(true)
                .senderHit(false)
                .recipientHit(false)
                .listsChecked(List.of("OFAC", "EU", "UN"))
                .provider("chainalysis")
                .providerRef("ref-sanctions-" + UUID.randomUUID())
                .screenedAt(Instant.now())
                .build();
    }

    private static AmlResult anAmlClearResult(UUID checkId) {
        return AmlResult.builder()
                .amlResultId(UUID.randomUUID())
                .checkId(checkId)
                .flagged(false)
                .provider("chainalysis")
                .providerRef("ref-aml-" + UUID.randomUUID())
                .screenedAt(Instant.now())
                .build();
    }

    private static TravelRulePackage aTravelRulePackage(UUID checkId) {
        return TravelRulePackage.builder()
                .packageId(UUID.randomUUID())
                .checkId(checkId)
                .originatorVasp(new VaspInfo("vasp-1", "StableBridge US", "US", "did:web:stablebridge.us"))
                .beneficiaryVasp(new VaspInfo("vasp-2", "StableBridge DE", "DE", "did:web:stablebridge.de"))
                .originatorData("{\"name\":\"John Doe\"}")
                .beneficiaryData("{\"name\":\"Hans Mueller\"}")
                .protocol(TravelRuleProtocol.IVMS101)
                .transmissionStatus(TransmissionStatus.PENDING)
                .build();
    }
}
