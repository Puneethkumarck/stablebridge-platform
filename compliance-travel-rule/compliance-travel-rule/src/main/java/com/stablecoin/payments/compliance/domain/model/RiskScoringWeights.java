package com.stablecoin.payments.compliance.domain.model;

import lombok.Builder;

import java.util.Map;

@Builder(toBuilder = true)
public record RiskScoringWeights(
        int kycTier1Penalty,
        int highValuePenalty,
        int amlFlagPenalty,
        int crossBorderPenalty,
        int newCustomerPenalty,
        int highCorridorRiskPenalty,
        int amountToLimitRatioPenalty,
        int highVelocityPenalty,
        Map<String, Integer> corridorRiskScores
) {
    public static final int DEFAULT_KYC_TIER1_PENALTY = 20;
    public static final int DEFAULT_HIGH_VALUE_PENALTY = 15;
    public static final int DEFAULT_AML_FLAG_PENALTY = 30;
    public static final int DEFAULT_CROSS_BORDER_PENALTY = 10;
    public static final int DEFAULT_NEW_CUSTOMER_PENALTY = 15;
    public static final int DEFAULT_HIGH_CORRIDOR_RISK_PENALTY = 20;
    public static final int DEFAULT_AMOUNT_TO_LIMIT_RATIO_PENALTY = 10;
    public static final int DEFAULT_HIGH_VELOCITY_PENALTY = 15;

    public RiskScoringWeights {
        if (corridorRiskScores == null) {
            corridorRiskScores = Map.of();
        }
    }

    public static RiskScoringWeights defaults() {
        return RiskScoringWeights.builder()
                .kycTier1Penalty(DEFAULT_KYC_TIER1_PENALTY)
                .highValuePenalty(DEFAULT_HIGH_VALUE_PENALTY)
                .amlFlagPenalty(DEFAULT_AML_FLAG_PENALTY)
                .crossBorderPenalty(DEFAULT_CROSS_BORDER_PENALTY)
                .newCustomerPenalty(DEFAULT_NEW_CUSTOMER_PENALTY)
                .highCorridorRiskPenalty(DEFAULT_HIGH_CORRIDOR_RISK_PENALTY)
                .amountToLimitRatioPenalty(DEFAULT_AMOUNT_TO_LIMIT_RATIO_PENALTY)
                .highVelocityPenalty(DEFAULT_HIGH_VELOCITY_PENALTY)
                .corridorRiskScores(Map.of())
                .build();
    }

    public int corridorRisk(String sourceCountry, String targetCountry) {
        var key = sourceCountry + "-" + targetCountry;
        return corridorRiskScores.getOrDefault(key, 0);
    }
}
