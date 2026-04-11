package com.stablecoin.payments.gateway.iam.application.security;

import com.stablecoin.payments.gateway.iam.domain.model.AuditLogEntry;
import com.stablecoin.payments.gateway.iam.domain.port.AuditLogRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.stablecoin.payments.gateway.iam.fixtures.TestUtils.eqIgnoring;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;

@ExtendWith(MockitoExtension.class)
class AuditLogFilterTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    @Mock
    private FilterChain filterChain;

    private AuditLogFilter filter;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        filter = new AuditLogFilter(auditLogRepository);
        request = new MockHttpServletRequest("POST", "/v1/payments");
        request.setRemoteAddr("10.0.0.1");
        response = new MockHttpServletResponse();
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldAlwaysCallFilterChain() throws ServletException, IOException {
        filter.doFilterInternal(request, response, filterChain);

        then(filterChain).should().doFilter(request, response);
    }

    @Test
    void shouldSkipAuditWhenNotAuthenticated() throws ServletException, IOException {
        filter.doFilterInternal(request, response, filterChain);

        then(auditLogRepository).shouldHaveNoInteractions();
    }

    @Nested
    class WhenAuthenticated {

        private final UUID merchantId = UUID.randomUUID();
        private final UUID clientId = UUID.randomUUID();

        @BeforeEach
        void setAuthentication() {
            SecurityContextHolder.getContext().setAuthentication(
                    new MerchantAuthentication(merchantId, clientId,
                            List.of("payments:read"), MerchantAuthentication.AuthMethod.API_KEY));
        }

        @Test
        void shouldPersistAuditLogEntry() throws ServletException, IOException {
            response.setStatus(201);

            filter.doFilterInternal(request, response, filterChain);

            var expectedEntry = AuditLogEntry.builder()
                    .merchantId(merchantId)
                    .action("POST")
                    .resource("/v1/payments")
                    .sourceIp("10.0.0.1")
                    .detail(Map.of(
                            "status_code", 201,
                            "auth_method", "API_KEY",
                            "client_id", clientId.toString()))
                    .build();
            then(auditLogRepository).should().save(eqIgnoring(expectedEntry, "logId", "occurredAt"));
        }

        @Test
        void shouldRecordJwtAuthMethod() throws ServletException, IOException {
            SecurityContextHolder.getContext().setAuthentication(
                    new MerchantAuthentication(merchantId, clientId,
                            List.of(), MerchantAuthentication.AuthMethod.JWT));

            filter.doFilterInternal(request, response, filterChain);

            var expectedEntry = AuditLogEntry.builder()
                    .merchantId(merchantId)
                    .action("POST")
                    .resource("/v1/payments")
                    .sourceIp("10.0.0.1")
                    .detail(Map.of(
                            "status_code", 200,
                            "auth_method", "JWT",
                            "client_id", clientId.toString()))
                    .build();
            then(auditLogRepository).should().save(eqIgnoring(expectedEntry, "logId", "occurredAt"));
        }

        @Test
        void shouldNotFailWhenRepositoryThrows() throws ServletException, IOException {
            var throwingEntry = AuditLogEntry.builder()
                    .merchantId(merchantId)
                    .action("POST")
                    .resource("/v1/payments")
                    .sourceIp("10.0.0.1")
                    .detail(Map.of(
                            "status_code", 200,
                            "auth_method", "API_KEY",
                            "client_id", clientId.toString()))
                    .build();
            willThrow(new RuntimeException("DB down"))
                    .given(auditLogRepository).save(eqIgnoring(throwingEntry, "logId", "occurredAt"));

            filter.doFilterInternal(request, response, filterChain);

            then(filterChain).should().doFilter(request, response);
        }

        @Test
        void shouldAuditEvenWhenFilterChainThrows() throws ServletException, IOException {
            willThrow(new ServletException("downstream error"))
                    .given(filterChain).doFilter(request, response);

            assertThatThrownBy(() -> filter.doFilterInternal(request, response, filterChain))
                    .isInstanceOf(ServletException.class);

            var expectedEntry = AuditLogEntry.builder()
                    .merchantId(merchantId)
                    .action("POST")
                    .resource("/v1/payments")
                    .sourceIp("10.0.0.1")
                    .detail(Map.of(
                            "status_code", 200,
                            "auth_method", "API_KEY",
                            "client_id", clientId.toString()))
                    .build();
            then(auditLogRepository).should().save(eqIgnoring(expectedEntry, "logId", "occurredAt"));
        }
    }
}
