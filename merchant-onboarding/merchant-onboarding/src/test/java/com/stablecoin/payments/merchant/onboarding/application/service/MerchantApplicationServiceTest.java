package com.stablecoin.payments.merchant.onboarding.application.service;

import com.stablecoin.payments.merchant.onboarding.api.request.ActivateMerchantRequest;
import com.stablecoin.payments.merchant.onboarding.api.request.ApproveCorridorRequest;
import com.stablecoin.payments.merchant.onboarding.api.request.CloseMerchantRequest;
import com.stablecoin.payments.merchant.onboarding.api.request.MerchantApplicationRequest;
import com.stablecoin.payments.merchant.onboarding.api.request.SuspendMerchantRequest;
import com.stablecoin.payments.merchant.onboarding.api.response.CorridorResponse;
import com.stablecoin.payments.merchant.onboarding.api.response.MerchantApplicationResponse;
import com.stablecoin.payments.merchant.onboarding.api.response.MerchantResponse;
import com.stablecoin.payments.merchant.onboarding.application.controller.MerchantRequestResponseMapper;
import com.stablecoin.payments.merchant.onboarding.domain.EventPublisher;
import com.stablecoin.payments.merchant.onboarding.domain.exceptions.MerchantNotFoundException;
import com.stablecoin.payments.merchant.onboarding.domain.merchant.ApprovedCorridorRepository;
import com.stablecoin.payments.merchant.onboarding.domain.merchant.CorridorEntitlementService;
import com.stablecoin.payments.merchant.onboarding.domain.merchant.DocumentStore;
import com.stablecoin.payments.merchant.onboarding.domain.merchant.KybProvider;
import com.stablecoin.payments.merchant.onboarding.domain.merchant.Merchant;
import com.stablecoin.payments.merchant.onboarding.domain.merchant.MerchantActivationPolicy;
import com.stablecoin.payments.merchant.onboarding.domain.merchant.MerchantRepository;
import com.stablecoin.payments.merchant.onboarding.domain.merchant.model.core.ApprovedCorridor;
import com.stablecoin.payments.merchant.onboarding.domain.merchant.model.core.KybStatus;
import com.stablecoin.payments.merchant.onboarding.domain.merchant.model.core.KybVerification;
import com.stablecoin.payments.merchant.onboarding.domain.merchant.model.events.MerchantActivatedEvent;
import com.stablecoin.payments.merchant.onboarding.domain.merchant.model.events.MerchantAppliedEvent;
import com.stablecoin.payments.merchant.onboarding.domain.merchant.model.events.MerchantClosedEvent;
import com.stablecoin.payments.merchant.onboarding.domain.merchant.model.events.MerchantCorridorApprovedEvent;
import com.stablecoin.payments.merchant.onboarding.domain.merchant.model.events.MerchantSuspendedEvent;
import com.stablecoin.payments.merchant.onboarding.fixtures.MerchantFixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
@DisplayName("MerchantApplicationService")
class MerchantApplicationServiceTest {

    @Mock
    private MerchantRepository merchantRepository;
    @Mock
    private KybProvider kybProvider;
    @Mock
    private EventPublisher<Object> eventPublisher;
    @Mock
    private MerchantRequestResponseMapper responseMapper;
    @Mock
    private MerchantActivationPolicy activationPolicy;
    @Mock
    private CorridorEntitlementService corridorEntitlementService;
    @Mock
    private DocumentStore documentStore;
    @Mock
    private ApprovedCorridorRepository approvedCorridorRepository;

    @InjectMocks
    private MerchantApplicationService service;

    @Captor
    private ArgumentCaptor<Object> eventCaptor;

    @Test
    @DisplayName("should apply merchant and publish event")
    void shouldApplyMerchant() {
        // given
        var request = new MerchantApplicationRequest(
                "Acme Ltd", "Acme", "REG-123", "GB", "PRIVATE_LIMITED",
                "https://acme.com", "USD", null, null, List.of("GB->US"));
        given(merchantRepository.existsByRegistrationNumberAndCountry("REG-123", "GB"))
                .willReturn(false);
        given(merchantRepository.save(any(Merchant.class)))
                .willAnswer(inv -> inv.getArgument(0));
        given(responseMapper.toApplicationResponse(any(Merchant.class)))
                .willReturn(new MerchantApplicationResponse(UUID.randomUUID(), "APPLIED", "NOT_STARTED", "Acme Ltd", Instant.now()));

        // when
        var result = service.apply(request);

        // then
        assertThat(result).isNotNull();
        assertThat(result.status()).isEqualTo("APPLIED");
        then(eventPublisher).should().publish(any(MerchantAppliedEvent.class));
    }

    @Test
    @DisplayName("should start KYB verification")
    void shouldStartKyb() {
        // given
        var merchant = MerchantFixtures.appliedMerchant();
        var merchantId = merchant.getMerchantId();
        given(merchantRepository.findById(merchantId)).willReturn(Optional.of(merchant));
        given(kybProvider.submit(any(), any(), any(), any()))
                .willReturn(KybVerification.builder()
                        .kybId(UUID.randomUUID())
                        .merchantId(merchantId)
                        .provider("mock")
                        .providerRef("ref-123")
                        .status(KybStatus.IN_PROGRESS)
                        .initiatedAt(Instant.now())
                        .build());
        given(merchantRepository.save(any(Merchant.class)))
                .willAnswer(inv -> inv.getArgument(0));

        // when
        service.startKyb(merchantId);

        // then
        then(merchantRepository).should().save(any(Merchant.class));
    }

    @Test
    @DisplayName("should activate merchant with policy validation")
    void shouldActivateMerchant() {
        // given
        var merchant = MerchantFixtures.pendingApprovalMerchant();
        var merchantId = merchant.getMerchantId();
        var request = new ActivateMerchantRequest(MerchantFixtures.anApprover(),
                List.of("payments:read", "payments:write"));
        given(merchantRepository.findById(merchantId)).willReturn(Optional.of(merchant));
        given(merchantRepository.save(any(Merchant.class)))
                .willAnswer(inv -> inv.getArgument(0));
        given(responseMapper.toMerchantResponse(any(Merchant.class)))
                .willReturn(merchantResponse(merchantId));

        // when
        var result = service.activate(merchantId, request);

        // then
        assertThat(result).isNotNull();
        then(activationPolicy).should().validate(merchant);
        then(eventPublisher).should().publish(any(MerchantActivatedEvent.class));
    }

    @Test
    @DisplayName("should reject activation when policy fails")
    void shouldRejectActivationWhenPolicyFails() {
        // given
        var merchant = MerchantFixtures.appliedMerchant();
        var merchantId = merchant.getMerchantId();
        var request = new ActivateMerchantRequest(MerchantFixtures.anApprover(), List.of("payments:read"));
        given(merchantRepository.findById(merchantId)).willReturn(Optional.of(merchant));
        org.mockito.BDDMockito.willThrow(new IllegalStateException("KYB not passed"))
                .given(activationPolicy).validate(any());

        // when / then
        assertThatThrownBy(() -> service.activate(merchantId, request))
                .isInstanceOf(IllegalStateException.class);
        then(eventPublisher).should(never()).publish(any());
    }

    @Test
    @DisplayName("should suspend merchant and publish event")
    void shouldSuspendMerchant() {
        // given
        var merchant = MerchantFixtures.activeMerchant();
        var merchantId = merchant.getMerchantId();
        var request = new SuspendMerchantRequest("compliance review", null);
        given(merchantRepository.findById(merchantId)).willReturn(Optional.of(merchant));
        given(merchantRepository.save(any(Merchant.class)))
                .willAnswer(inv -> inv.getArgument(0));

        // when
        service.suspend(merchantId, request);

        // then
        then(eventPublisher).should().publish(any(MerchantSuspendedEvent.class));
    }

    @Test
    @DisplayName("should reactivate merchant")
    void shouldReactivateMerchant() {
        // given
        var merchant = MerchantFixtures.suspendedMerchant();
        var merchantId = merchant.getMerchantId();
        given(merchantRepository.findById(merchantId)).willReturn(Optional.of(merchant));
        given(merchantRepository.save(any(Merchant.class)))
                .willAnswer(inv -> inv.getArgument(0));

        // when
        service.reactivate(merchantId);

        // then
        then(merchantRepository).should().save(any(Merchant.class));
    }

    @Test
    @DisplayName("should close merchant and publish event")
    void shouldCloseMerchant() {
        // given
        var merchant = MerchantFixtures.activeMerchant();
        var merchantId = merchant.getMerchantId();
        var request = new CloseMerchantRequest("business closure", null);
        given(merchantRepository.findById(merchantId)).willReturn(Optional.of(merchant));
        given(merchantRepository.save(any(Merchant.class)))
                .willAnswer(inv -> inv.getArgument(0));

        // when
        service.close(merchantId, request);

        // then
        then(eventPublisher).should().publish(any(MerchantClosedEvent.class));
    }

    @Test
    @DisplayName("should approve corridor with entitlement validation")
    void shouldApproveCorridor() {
        // given
        var merchant = MerchantFixtures.activeMerchant();
        var merchantId = merchant.getMerchantId();
        var approvedBy = MerchantFixtures.anApprover();
        var request = new ApproveCorridorRequest("GB", "US", List.of("GBP", "USD"),
                new BigDecimal("100000"), Instant.now().plusSeconds(86400));
        given(merchantRepository.findById(merchantId)).willReturn(Optional.of(merchant));
        given(approvedCorridorRepository.save(any(ApprovedCorridor.class)))
                .willAnswer(inv -> inv.getArgument(0));
        given(responseMapper.toCorridorResponse(any(ApprovedCorridor.class)))
                .willReturn(new CorridorResponse(UUID.randomUUID(), merchantId, "GB", "US",
                        List.of("GBP", "USD"), new BigDecimal("100000"), approvedBy,
                        Instant.now(), request.expiresAt(), true));

        // when
        var result = service.approveCorridor(merchantId, request, approvedBy);

        // then
        assertThat(result).isNotNull();
        then(corridorEntitlementService).should().validate(merchant, "GB", "US");
        then(eventPublisher).should().publish(any(MerchantCorridorApprovedEvent.class));
    }

    @Test
    @DisplayName("should throw when merchant not found")
    void shouldThrowWhenMerchantNotFound() {
        // given
        var merchantId = UUID.randomUUID();
        given(merchantRepository.findById(merchantId)).willReturn(Optional.empty());

        // when / then
        assertThatThrownBy(() -> service.findById(merchantId))
                .isInstanceOf(MerchantNotFoundException.class);
    }

    private MerchantResponse merchantResponse(UUID merchantId) {
        return new MerchantResponse(
                merchantId, "Acme Ltd", "Acme", "REG-123", "GB",
                "PRIVATE_LIMITED", "https://acme.com", "USD",
                "ACTIVE", "PASSED", "LOW", "GROWTH",
                List.of("payments:read"), List.of("GB->US"),
                Instant.now(), Instant.now(), Instant.now());
    }
}
