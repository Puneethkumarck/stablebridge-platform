package com.stablecoin.payments.merchant.onboarding.domain.merchant;

import com.stablecoin.payments.merchant.onboarding.domain.merchant.model.core.RiskTier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RiskTierCalculator")
class RiskTierCalculatorTest {

    private final RiskTierCalculator calculator = new RiskTierCalculator();

    @ParameterizedTest(name = "score {0} → {1}")
    @CsvSource({
            "0, LOW",
            "10, LOW",
            "25, LOW",
            "26, MEDIUM",
            "35, MEDIUM",
            "50, MEDIUM",
            "51, HIGH",
            "75, HIGH",
            "100, HIGH"
    })
    @DisplayName("should map risk score to correct tier")
    void shouldMapRiskScoreToCorrectTier(int score, RiskTier expectedTier) {
        var result = calculator.calculate(Map.of("risk_score", score));
        assertThat(result).isEqualTo(expectedTier);
    }

    @Test
    @DisplayName("should return LOW when risk signals are null")
    void shouldReturnLowWhenRiskSignalsNull() {
        var result = calculator.calculate(null);
        assertThat(result).isEqualTo(RiskTier.LOW);
    }

    @Test
    @DisplayName("should return LOW when risk_score key is missing")
    void shouldReturnLowWhenRiskScoreKeyMissing() {
        var result = calculator.calculate(Map.of("other_key", 99));
        assertThat(result).isEqualTo(RiskTier.LOW);
    }
}
