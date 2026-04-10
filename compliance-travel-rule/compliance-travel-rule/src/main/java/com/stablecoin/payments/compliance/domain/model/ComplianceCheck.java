package com.stablecoin.payments.compliance.domain.model;

import com.stablecoin.payments.compliance.domain.statemachine.StateMachine;
import com.stablecoin.payments.compliance.domain.statemachine.StateTransition;
import lombok.AccessLevel;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static com.stablecoin.payments.compliance.domain.model.ComplianceCheckStatus.AML_SCREENING;
import static com.stablecoin.payments.compliance.domain.model.ComplianceCheckStatus.FAILED;
import static com.stablecoin.payments.compliance.domain.model.ComplianceCheckStatus.KYC_IN_PROGRESS;
import static com.stablecoin.payments.compliance.domain.model.ComplianceCheckStatus.MANUAL_REVIEW;
import static com.stablecoin.payments.compliance.domain.model.ComplianceCheckStatus.PASSED;
import static com.stablecoin.payments.compliance.domain.model.ComplianceCheckStatus.PENDING;
import static com.stablecoin.payments.compliance.domain.model.ComplianceCheckStatus.RISK_SCORING;
import static com.stablecoin.payments.compliance.domain.model.ComplianceCheckStatus.SANCTIONS_HIT;
import static com.stablecoin.payments.compliance.domain.model.ComplianceCheckStatus.SANCTIONS_SCREENING;
import static com.stablecoin.payments.compliance.domain.model.ComplianceCheckStatus.TRAVEL_RULE_PACKAGING;
import static com.stablecoin.payments.compliance.domain.model.ComplianceCheckTrigger.AML_CLEAR;
import static com.stablecoin.payments.compliance.domain.model.ComplianceCheckTrigger.AML_FLAGGED;
import static com.stablecoin.payments.compliance.domain.model.ComplianceCheckTrigger.ESCALATE_MANUAL_REVIEW;
import static com.stablecoin.payments.compliance.domain.model.ComplianceCheckTrigger.KYC_FAILED;
import static com.stablecoin.payments.compliance.domain.model.ComplianceCheckTrigger.KYC_PASSED;
import static com.stablecoin.payments.compliance.domain.model.ComplianceCheckTrigger.RISK_CRITICAL;
import static com.stablecoin.payments.compliance.domain.model.ComplianceCheckTrigger.RISK_SCORED;
import static com.stablecoin.payments.compliance.domain.model.ComplianceCheckTrigger.SANCTIONS_CLEAR;
import static com.stablecoin.payments.compliance.domain.model.ComplianceCheckTrigger.SANCTIONS_HIT_DETECTED;
import static com.stablecoin.payments.compliance.domain.model.ComplianceCheckTrigger.START_KYC;
import static com.stablecoin.payments.compliance.domain.model.ComplianceCheckTrigger.TRAVEL_RULE_COMPLETE;
import static com.stablecoin.payments.compliance.domain.model.ComplianceCheckTrigger.TRAVEL_RULE_FAILED;

@Builder(toBuilder = true, access = AccessLevel.PACKAGE)
public record ComplianceCheck(
        UUID checkId,
        UUID paymentId,
        UUID correlationId,
        UUID senderId,
        UUID recipientId,
        BigDecimal sourceAmount,
        String sourceCurrency,
        String targetCurrency,
        String sourceCountry,
        String targetCountry,
        ComplianceCheckStatus status,
        OverallResult overallResult,
        RiskScore riskScore,
        KycResult kycResult,
        SanctionsResult sanctionsResult,
        AmlResult amlResult,
        TravelRulePackage travelRulePackage,
        String errorCode,
        String errorMessage,
        Instant createdAt,
        Instant completedAt,
        Instant expiresAt
) {

    private static final Set<ComplianceCheckStatus> TERMINAL_STATES =
            Set.of(PASSED, FAILED, SANCTIONS_HIT, MANUAL_REVIEW);

    private static final StateMachine<ComplianceCheckStatus, ComplianceCheckTrigger> STATE_MACHINE =
            new StateMachine<>(List.of(
                    // Happy path
                    new StateTransition<>(PENDING, START_KYC, KYC_IN_PROGRESS),
                    new StateTransition<>(KYC_IN_PROGRESS, KYC_PASSED, SANCTIONS_SCREENING),
                    new StateTransition<>(SANCTIONS_SCREENING, SANCTIONS_CLEAR, AML_SCREENING),
                    new StateTransition<>(AML_SCREENING, AML_CLEAR, RISK_SCORING),
                    new StateTransition<>(RISK_SCORING, RISK_SCORED, TRAVEL_RULE_PACKAGING),
                    new StateTransition<>(TRAVEL_RULE_PACKAGING, TRAVEL_RULE_COMPLETE, PASSED),
                    // Risk critical path
                    new StateTransition<>(RISK_SCORING, RISK_CRITICAL, MANUAL_REVIEW),
                    // Failure paths
                    new StateTransition<>(KYC_IN_PROGRESS, KYC_FAILED, FAILED),
                    new StateTransition<>(SANCTIONS_SCREENING, SANCTIONS_HIT_DETECTED, SANCTIONS_HIT),
                    new StateTransition<>(AML_SCREENING, AML_FLAGGED, MANUAL_REVIEW),
                    new StateTransition<>(TRAVEL_RULE_PACKAGING, TRAVEL_RULE_FAILED, FAILED),
                    // Sanctions hit escalation
                    new StateTransition<>(SANCTIONS_HIT, ESCALATE_MANUAL_REVIEW, MANUAL_REVIEW)
            ));


    public static ComplianceCheck initiate(UUID paymentId, UUID senderId, UUID recipientId,
                                           Money sourceAmount, String sourceCountry,
                                           String targetCountry, String targetCurrency) {
        if (paymentId == null) {
            throw new IllegalArgumentException("paymentId is required");
        }
        if (senderId == null) {
            throw new IllegalArgumentException("senderId is required");
        }
        if (recipientId == null) {
            throw new IllegalArgumentException("recipientId is required");
        }
        if (sourceAmount == null) {
            throw new IllegalArgumentException("sourceAmount is required");
        }
        if (sourceCountry == null || sourceCountry.isBlank()) {
            throw new IllegalArgumentException("sourceCountry is required");
        }
        if (targetCountry == null || targetCountry.isBlank()) {
            throw new IllegalArgumentException("targetCountry is required");
        }
        if (targetCurrency == null || targetCurrency.isBlank()) {
            throw new IllegalArgumentException("targetCurrency is required");
        }

        var now = Instant.now();
        return ComplianceCheck.builder()
                .checkId(UUID.randomUUID())
                .paymentId(paymentId)
                .correlationId(UUID.randomUUID())
                .senderId(senderId)
                .recipientId(recipientId)
                .sourceAmount(sourceAmount.amount())
                .sourceCurrency(sourceAmount.currency())
                .targetCurrency(targetCurrency)
                .sourceCountry(sourceCountry)
                .targetCountry(targetCountry)
                .status(PENDING)
                .createdAt(now)
                .expiresAt(now.plusSeconds(300))
                .build();
    }


    public ComplianceCheck startKyc() {
        assertNotTerminal();
        var nextStatus = STATE_MACHINE.transition(status, START_KYC);
        return toBuilder()
                .status(nextStatus)
                .build();
    }

    public ComplianceCheck passKyc(KycResult result) {
        assertNotTerminal();
        if (result == null) {
            throw new IllegalArgumentException("KYC result is required");
        }
        var nextStatus = STATE_MACHINE.transition(status, KYC_PASSED);
        return toBuilder()
                .status(nextStatus)
                .kycResult(result)
                .build();
    }

    public ComplianceCheck failKyc(KycResult result) {
        assertNotTerminal();
        if (result == null) {
            throw new IllegalArgumentException("KYC result is required");
        }
        var nextStatus = STATE_MACHINE.transition(status, KYC_FAILED);
        return toBuilder()
                .status(nextStatus)
                .kycResult(result)
                .overallResult(OverallResult.FAILED)
                .errorCode("CO-1001")
                .errorMessage("KYC verification failed")
                .completedAt(Instant.now())
                .build();
    }

    public ComplianceCheck sanctionsClear(SanctionsResult result) {
        assertNotTerminal();
        if (result == null) {
            throw new IllegalArgumentException("Sanctions result is required");
        }
        var nextStatus = STATE_MACHINE.transition(status, SANCTIONS_CLEAR);
        return toBuilder()
                .status(nextStatus)
                .sanctionsResult(result)
                .build();
    }

    public ComplianceCheck sanctionsHitDetected(SanctionsResult result) {
        assertNotTerminal();
        if (result == null) {
            throw new IllegalArgumentException("Sanctions result is required");
        }
        var nextStatus = STATE_MACHINE.transition(status, SANCTIONS_HIT_DETECTED);
        return toBuilder()
                .status(nextStatus)
                .sanctionsResult(result)
                .overallResult(OverallResult.SANCTIONS_HIT)
                .errorCode("CO-2001")
                .errorMessage("Sanctions screening hit detected")
                .completedAt(Instant.now())
                .build();
    }

    public ComplianceCheck amlClear(AmlResult result) {
        assertNotTerminal();
        if (result == null) {
            throw new IllegalArgumentException("AML result is required");
        }
        var nextStatus = STATE_MACHINE.transition(status, AML_CLEAR);
        return toBuilder()
                .status(nextStatus)
                .amlResult(result)
                .build();
    }

    public ComplianceCheck amlFlagged(AmlResult result) {
        assertNotTerminal();
        if (result == null) {
            throw new IllegalArgumentException("AML result is required");
        }
        var nextStatus = STATE_MACHINE.transition(status, AML_FLAGGED);
        return toBuilder()
                .status(nextStatus)
                .amlResult(result)
                .overallResult(OverallResult.MANUAL_REVIEW)
                .errorCode("CO-3001")
                .errorMessage("AML screening flagged — manual review required")
                .completedAt(Instant.now())
                .build();
    }

    public ComplianceCheck riskScored(RiskScore score) {
        assertNotTerminal();
        if (score == null) {
            throw new IllegalArgumentException("Risk score is required");
        }
        var nextStatus = STATE_MACHINE.transition(status, RISK_SCORED);
        return toBuilder()
                .status(nextStatus)
                .riskScore(score)
                .build();
    }

    public ComplianceCheck riskCritical(RiskScore score) {
        assertNotTerminal();
        if (score == null) {
            throw new IllegalArgumentException("Risk score is required");
        }
        var nextStatus = STATE_MACHINE.transition(status, RISK_CRITICAL);
        return toBuilder()
                .status(nextStatus)
                .riskScore(score)
                .overallResult(OverallResult.MANUAL_REVIEW)
                .errorCode("CO-4001")
                .errorMessage("Critical risk score — manual review required")
                .completedAt(Instant.now())
                .build();
    }

    public ComplianceCheck completeTravelRule(TravelRulePackage travelRule) {
        assertNotTerminal();
        var nextStatus = STATE_MACHINE.transition(status, TRAVEL_RULE_COMPLETE);
        return toBuilder()
                .status(nextStatus)
                .travelRulePackage(travelRule)
                .overallResult(OverallResult.PASSED)
                .completedAt(Instant.now())
                .build();
    }

    public ComplianceCheck failTravelRule(String reason) {
        assertNotTerminal();
        var nextStatus = STATE_MACHINE.transition(status, TRAVEL_RULE_FAILED);
        return toBuilder()
                .status(nextStatus)
                .overallResult(OverallResult.FAILED)
                .errorCode("CO-5001")
                .errorMessage(reason != null ? reason : "Travel rule transmission failed")
                .completedAt(Instant.now())
                .build();
    }

    public ComplianceCheck escalateToManualReview() {
        var nextStatus = STATE_MACHINE.transition(status, ESCALATE_MANUAL_REVIEW);
        return toBuilder()
                .status(nextStatus)
                .overallResult(OverallResult.MANUAL_REVIEW)
                .build();
    }


    public boolean isTerminal() {
        return TERMINAL_STATES.contains(status);
    }

    public boolean canApply(ComplianceCheckTrigger trigger) {
        return STATE_MACHINE.canTransition(status, trigger);
    }

    public boolean allSubChecksPassed() {
        return kycResult != null
                && kycResult.senderStatus() == KycStatus.VERIFIED
                && sanctionsResult != null
                && !sanctionsResult.senderHit()
                && !sanctionsResult.recipientHit()
                && amlResult != null
                && !amlResult.flagged()
                && riskScore != null;
    }


    private void assertNotTerminal() {
        if (isTerminal()) {
            throw new IllegalStateException(
                    "ComplianceCheck %s is in terminal state %s and cannot be modified"
                            .formatted(checkId, status));
        }
    }
}
