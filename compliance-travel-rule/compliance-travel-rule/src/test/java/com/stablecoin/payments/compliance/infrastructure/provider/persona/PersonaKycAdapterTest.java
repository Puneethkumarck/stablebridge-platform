package com.stablecoin.payments.compliance.infrastructure.provider.persona;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import com.stablecoin.payments.compliance.domain.model.KycStatus;
import com.stablecoin.payments.compliance.domain.model.KycTier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@WireMockTest
@DisplayName("PersonaKycAdapter")
class PersonaKycAdapterTest {

    private PersonaKycAdapter adapter;

    private static final UUID SENDER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID RECIPIENT_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @BeforeEach
    void setUp(WireMockRuntimeInfo wmInfo) {
        var properties = new PersonaProperties(
                wmInfo.getHttpBaseUrl(),
                "persona_sandbox_test_key",
                "itmpl_test_template",
                "2023-01-05",
                5
        );
        adapter = new PersonaKycAdapter(properties);
    }

    private void stubInquiryCreation(String status) {
        stubFor(post(urlEqualTo("/api/v1/inquiries"))
                .withHeader("Authorization", equalTo("Bearer persona_sandbox_test_key"))
                .withHeader("Persona-Version", equalTo("2023-01-05"))
                .willReturn(aResponse()
                        .withStatus(201)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "data": {
                                    "id": "inq_test_123",
                                    "type": "inquiry",
                                    "attributes": {
                                      "status": "%s",
                                      "reference-id": "test-ref",
                                      "created-at": "2026-03-18T10:00:00Z"
                                    }
                                  }
                                }
                                """.formatted(status))));
    }

    @Nested
    @DisplayName("Approved inquiry")
    class ApprovedInquiry {

        @Test
        @DisplayName("should return VERIFIED when inquiry status is approved")
        void shouldReturnVerifiedWhenApproved() {
            stubInquiryCreation("approved");

            var result = adapter.verify(SENDER_ID, RECIPIENT_ID);

            assertThat(result.senderStatus()).isEqualTo(KycStatus.VERIFIED);
            assertThat(result.recipientStatus()).isEqualTo(KycStatus.VERIFIED);
            assertThat(result.senderKycTier()).isEqualTo(KycTier.KYC_TIER_2);
            assertThat(result.provider()).isEqualTo("persona");
        }
    }

    @Nested
    @DisplayName("Completed inquiry")
    class CompletedInquiry {

        @Test
        @DisplayName("should return VERIFIED when inquiry status is completed")
        void shouldReturnVerifiedWhenCompleted() {
            stubInquiryCreation("completed");

            var result = adapter.verify(SENDER_ID, RECIPIENT_ID);

            assertThat(result.senderStatus()).isEqualTo(KycStatus.VERIFIED);
            assertThat(result.recipientStatus()).isEqualTo(KycStatus.VERIFIED);
        }
    }

    @Nested
    @DisplayName("Pending inquiry")
    class PendingInquiry {

        @Test
        @DisplayName("should return PENDING when inquiry status is pending")
        void shouldReturnPendingWhenPending() {
            stubInquiryCreation("pending");

            var result = adapter.verify(SENDER_ID, RECIPIENT_ID);

            assertThat(result.senderStatus()).isEqualTo(KycStatus.PENDING);
            assertThat(result.recipientStatus()).isEqualTo(KycStatus.PENDING);
            assertThat(result.senderKycTier()).isEqualTo(KycTier.KYC_TIER_1);
        }
    }

    @Nested
    @DisplayName("Declined inquiry")
    class DeclinedInquiry {

        @Test
        @DisplayName("should return REJECTED when inquiry status is declined")
        void shouldReturnRejectedWhenDeclined() {
            stubInquiryCreation("declined");

            var result = adapter.verify(SENDER_ID, RECIPIENT_ID);

            assertThat(result.senderStatus()).isEqualTo(KycStatus.REJECTED);
            assertThat(result.recipientStatus()).isEqualTo(KycStatus.REJECTED);
        }
    }

    @Nested
    @DisplayName("Error handling")
    class ErrorHandling {

        @Test
        @DisplayName("should throw when API returns server error")
        void shouldThrowOnServerError() {
            stubFor(post(urlEqualTo("/api/v1/inquiries"))
                    .willReturn(aResponse().withStatus(500)));

            assertThatThrownBy(() -> adapter.verify(SENDER_ID, RECIPIENT_ID))
                    .isInstanceOf(Exception.class);
        }

        @Test
        @DisplayName("should throw when API returns 401 unauthorized")
        void shouldThrowOnUnauthorized() {
            stubFor(post(urlEqualTo("/api/v1/inquiries"))
                    .willReturn(aResponse().withStatus(401)));

            assertThatThrownBy(() -> adapter.verify(SENDER_ID, RECIPIENT_ID))
                    .isInstanceOf(Exception.class);
        }
    }
}
