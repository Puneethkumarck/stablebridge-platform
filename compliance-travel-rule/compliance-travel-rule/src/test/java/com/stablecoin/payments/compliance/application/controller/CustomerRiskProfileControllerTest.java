package com.stablecoin.payments.compliance.application.controller;

import com.stablecoin.payments.compliance.api.response.CustomerRiskProfileResponse;
import com.stablecoin.payments.compliance.application.mapper.ComplianceCheckResponseMapper;
import com.stablecoin.payments.compliance.domain.exception.CustomerNotFoundException;
import com.stablecoin.payments.compliance.domain.service.ComplianceCheckCommandHandler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static com.stablecoin.payments.compliance.fixtures.CustomerRiskProfileFixtures.aRiskProfile;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("CustomerRiskProfileController")
class CustomerRiskProfileControllerTest {

    @Mock
    private ComplianceCheckCommandHandler commandHandler;

    @Mock
    private ComplianceCheckResponseMapper responseMapper;

    @InjectMocks
    private CustomerRiskProfileController controller;

    @Test
    @DisplayName("should delegate to command handler and return risk profile response")
    void shouldGetRiskProfile() {
        var customerId = UUID.randomUUID();
        var profile = aRiskProfile().toBuilder().customerId(customerId).build();
        var now = Instant.now();
        var expectedResponse = new CustomerRiskProfileResponse(
                customerId, "KYC_TIER_2", now, "LOW", 20,
                new BigDecimal("10000.00"), new BigDecimal("50000.00"),
                new BigDecimal("500000.00"), now);

        given(commandHandler.getCustomerRiskProfile(customerId)).willReturn(profile);
        given(responseMapper.toResponse(profile)).willReturn(expectedResponse);

        var result = controller.getRiskProfile(customerId);

        assertThat(result)
                .usingRecursiveComparison()
                .isEqualTo(expectedResponse);

        then(commandHandler).should().getCustomerRiskProfile(customerId);
        then(responseMapper).should().toResponse(profile);
    }

    @Test
    @DisplayName("should propagate CustomerNotFoundException")
    void shouldPropagateOnNotFound() {
        var customerId = UUID.randomUUID();
        given(commandHandler.getCustomerRiskProfile(customerId))
                .willThrow(new CustomerNotFoundException(customerId));

        assertThatThrownBy(() -> controller.getRiskProfile(customerId))
                .isInstanceOf(CustomerNotFoundException.class)
                .hasMessageContaining(customerId.toString());
    }
}
