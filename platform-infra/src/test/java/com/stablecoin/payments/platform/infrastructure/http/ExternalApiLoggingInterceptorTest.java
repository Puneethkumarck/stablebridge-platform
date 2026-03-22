package com.stablecoin.payments.platform.infrastructure.http;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ExternalApiLoggingInterceptor")
class ExternalApiLoggingInterceptorTest {

    @Nested
    @DisplayName("redact")
    class Redact {

        @Test
        @DisplayName("should redact long value keeping first 8 chars")
        void shouldRedactLongValue() {
            assertThat(ExternalApiLoggingInterceptor.redact("sk_test_51TCRge3nnME1dfOB"))
                    .isEqualTo("sk_test_***");
        }

        @Test
        @DisplayName("should fully redact short value")
        void shouldFullyRedactShortValue() {
            assertThat(ExternalApiLoggingInterceptor.redact("short"))
                    .isEqualTo("***");
        }

        @Test
        @DisplayName("should return *** for null")
        void shouldReturnStarsForNull() {
            assertThat(ExternalApiLoggingInterceptor.redact(null))
                    .isEqualTo("***");
        }

        @Test
        @DisplayName("should redact exactly 8-char boundary")
        void shouldRedactExactBoundary() {
            assertThat(ExternalApiLoggingInterceptor.redact("12345678"))
                    .isEqualTo("***");
        }

        @Test
        @DisplayName("should keep first 8 chars for 9-char value")
        void shouldKeepFirstEightForNineChars() {
            assertThat(ExternalApiLoggingInterceptor.redact("123456789"))
                    .isEqualTo("12345678***");
        }
    }
}
