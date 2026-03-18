package com.stablecoin.payments.compliance.infrastructure.provider.ofacsdn;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class JaroWinklerSimilarityTest {

    @Nested
    @DisplayName("Exact matches")
    class ExactMatches {

        @Test
        @DisplayName("should return 1.0 for identical strings")
        void identicalStrings() {
            assertThat(JaroWinklerSimilarity.similarity("Viktor Petrov", "Viktor Petrov"))
                    .isCloseTo(1.0, within(0.001));
        }

        @Test
        @DisplayName("should return 1.0 for case-insensitive match after normalization")
        void caseInsensitiveMatch() {
            assertThat(JaroWinklerSimilarity.similarity("VIKTOR PETROV", "viktor petrov"))
                    .isCloseTo(1.0, within(0.001));
        }

        @Test
        @DisplayName("should return 1.0 for strings differing only in whitespace")
        void whitespaceDifference() {
            assertThat(JaroWinklerSimilarity.similarity("Viktor  Petrov", "Viktor Petrov"))
                    .isCloseTo(1.0, within(0.001));
        }
    }

    @Nested
    @DisplayName("Fuzzy matches")
    class FuzzyMatches {

        @Test
        @DisplayName("should return high score for similar names with minor spelling difference")
        void minorSpellingDifference() {
            double score = JaroWinklerSimilarity.similarity("Viktor Petrov", "Victor Petroff");
            assertThat(score).isGreaterThan(0.80);
        }

        @Test
        @DisplayName("should return high score for abbreviated vs full name")
        void abbreviatedName() {
            double score = JaroWinklerSimilarity.similarity("Ahmad Al-Rashid", "Ahmed Al-Rasheed");
            assertThat(score).isGreaterThan(0.80);
        }
    }

    @Nested
    @DisplayName("Non-matches")
    class NonMatches {

        @Test
        @DisplayName("should return low score for completely different strings")
        void completelyDifferent() {
            double score = JaroWinklerSimilarity.similarity("Viktor Petrov", "John Smith");
            assertThat(score).isLessThan(0.70);
        }

        @Test
        @DisplayName("should return 0.0 for null input")
        void nullInput() {
            assertThat(JaroWinklerSimilarity.similarity(null, "Viktor")).isCloseTo(0.0, within(0.001));
            assertThat(JaroWinklerSimilarity.similarity("Viktor", null)).isCloseTo(0.0, within(0.001));
        }

        @Test
        @DisplayName("should return 0.0 for empty strings")
        void emptyStrings() {
            assertThat(JaroWinklerSimilarity.similarity("", "Viktor")).isCloseTo(0.0, within(0.001));
            assertThat(JaroWinklerSimilarity.similarity("Viktor", "")).isCloseTo(0.0, within(0.001));
        }
    }

    @Nested
    @DisplayName("Normalization")
    class Normalization {

        @Test
        @DisplayName("should normalize to lowercase trimmed single-space")
        void normalizesCorrectly() {
            assertThat(JaroWinklerSimilarity.normalize("  Viktor   PETROV  ")).isEqualTo("viktor petrov");
        }

        @Test
        @DisplayName("should return empty for null input")
        void nullReturnsEmpty() {
            assertThat(JaroWinklerSimilarity.normalize(null)).isEqualTo("");
        }
    }
}
