package com.stablecoin.payments.compliance.application.controller;

import com.stablecoin.payments.compliance.api.request.InitiateComplianceCheckRequest;
import com.stablecoin.payments.compliance.api.response.ComplianceCheckResponse;
import com.stablecoin.payments.compliance.api.response.ComplianceCheckResponse.KycResultResponse;
import com.stablecoin.payments.compliance.api.response.ComplianceCheckResponse.RiskScoreResponse;
import com.stablecoin.payments.compliance.api.response.ComplianceCheckResponse.SanctionsResultResponse;
import com.stablecoin.payments.compliance.api.response.ComplianceCheckResponse.TravelRuleResponse;
import com.stablecoin.payments.compliance.application.mapper.ComplianceCheckResponseMapper;
import com.stablecoin.payments.compliance.domain.exception.CheckNotFoundException;
import com.stablecoin.payments.compliance.domain.exception.DuplicatePaymentException;
import com.stablecoin.payments.compliance.domain.model.Money;
import com.stablecoin.payments.compliance.domain.service.ComplianceCheckCommandHandler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static com.stablecoin.payments.compliance.fixtures.ComplianceCheckFixtures.aPendingCheck;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("ComplianceCheckController")
class ComplianceCheckControllerTest {

    @Mock
    private ComplianceCheckCommandHandler commandHandler;

    @Mock
    private ComplianceCheckResponseMapper responseMapper;

    @InjectMocks
    private ComplianceCheckController controller;

    @Nested
    @DisplayName("POST /v1/compliance/check")
    class InitiateCheck {

        @Test
        @DisplayName("should delegate to command handler and map response")
        void shouldInitiateCheck() {
            var request = new InitiateComplianceCheckRequest(
                    UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                    new BigDecimal("1000.00"), "USD", "US", "DE", "EUR");

            var domainCheck = aPendingCheck();
            var now = Instant.now();
            var expectedResponse = new ComplianceCheckResponse(
                    domainCheck.checkId(), request.paymentId(), "PASSED", "PASSED",
                    new RiskScoreResponse(18, "LOW", List.of("ESTABLISHED_CUSTOMER")),
                    new KycResultResponse("VERIFIED", "VERIFIED", "KYC_TIER_2"),
                    new SanctionsResultResponse(false, false, List.of("OFAC", "EU", "UN")),
                    new TravelRuleResponse("IVMS101", "TRANSMITTED"),
                    null, null, now, now);

            given(commandHandler.initiateCheck(
                    request.paymentId(), request.senderId(), request.recipientId(),
                    new Money(request.amount(), request.currency()),
                    request.sourceCountry(), request.targetCountry(), request.targetCurrency()))
                    .willReturn(domainCheck);
            given(responseMapper.toResponse(domainCheck)).willReturn(expectedResponse);

            var result = controller.initiateCheck(request);

            assertThat(result)
                    .usingRecursiveComparison()
                    .isEqualTo(expectedResponse);

            then(commandHandler).should().initiateCheck(
                    request.paymentId(), request.senderId(), request.recipientId(),
                    new Money(request.amount(), request.currency()),
                    request.sourceCountry(), request.targetCountry(), request.targetCurrency());
            then(responseMapper).should().toResponse(domainCheck);
        }

        @Test
        @DisplayName("should propagate DuplicatePaymentException")
        void shouldPropagateOnDuplicate() {
            var paymentId = UUID.randomUUID();
            var request = new InitiateComplianceCheckRequest(
                    paymentId, UUID.randomUUID(), UUID.randomUUID(),
                    new BigDecimal("1000.00"), "USD", "US", "DE", "EUR");

            given(commandHandler.initiateCheck(
                    request.paymentId(), request.senderId(), request.recipientId(),
                    new Money(request.amount(), request.currency()),
                    request.sourceCountry(), request.targetCountry(), request.targetCurrency()))
                    .willThrow(new DuplicatePaymentException(paymentId));

            assertThatThrownBy(() -> controller.initiateCheck(request))
                    .isInstanceOf(DuplicatePaymentException.class)
                    .hasMessageContaining(paymentId.toString());
        }
    }

    @Nested
    @DisplayName("GET /v1/compliance/checks/{checkId}")
    class GetCheck {

        @Test
        @DisplayName("should delegate to command handler and map response")
        void shouldGetCheck() {
            var checkId = UUID.randomUUID();
            var domainCheck = aPendingCheck();
            var now = Instant.now();
            var expectedResponse = new ComplianceCheckResponse(
                    checkId, domainCheck.paymentId(), "PENDING", null,
                    null, null, null, null,
                    null, null, now, null);

            given(commandHandler.getCheck(checkId)).willReturn(domainCheck);
            given(responseMapper.toResponse(domainCheck)).willReturn(expectedResponse);

            var result = controller.getCheck(checkId);

            assertThat(result)
                    .usingRecursiveComparison()
                    .isEqualTo(expectedResponse);

            then(commandHandler).should().getCheck(checkId);
            then(responseMapper).should().toResponse(domainCheck);
        }

        @Test
        @DisplayName("should propagate CheckNotFoundException")
        void shouldPropagateOnNotFound() {
            var checkId = UUID.randomUUID();
            given(commandHandler.getCheck(checkId))
                    .willThrow(new CheckNotFoundException(checkId));

            assertThatThrownBy(() -> controller.getCheck(checkId))
                    .isInstanceOf(CheckNotFoundException.class)
                    .hasMessageContaining(checkId.toString());
        }
    }
}
