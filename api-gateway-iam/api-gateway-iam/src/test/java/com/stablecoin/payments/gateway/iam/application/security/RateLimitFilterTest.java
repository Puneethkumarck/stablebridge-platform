package com.stablecoin.payments.gateway.iam.application.security;

import com.stablecoin.payments.gateway.iam.domain.model.Merchant;
import com.stablecoin.payments.gateway.iam.domain.model.MerchantStatus;
import com.stablecoin.payments.gateway.iam.domain.model.RateLimitEvent;
import com.stablecoin.payments.gateway.iam.domain.model.RateLimitPolicy;
import com.stablecoin.payments.gateway.iam.domain.model.RateLimitTier;
import com.stablecoin.payments.gateway.iam.domain.port.MerchantRepository;
import com.stablecoin.payments.gateway.iam.domain.port.RateLimitEventRepository;
import com.stablecoin.payments.gateway.iam.domain.port.RateLimiter;
import com.stablecoin.payments.gateway.iam.domain.port.RateLimiter.RateLimitResult;
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
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.stablecoin.payments.gateway.iam.fixtures.TestUtils.eqIgnoring;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class RateLimitFilterTest {

    @Mock
    private RateLimiter rateLimiter;

    @Mock
    private MerchantRepository merchantRepository;

    @Mock
    private RateLimitEventRepository rateLimitEventRepository;

    @Mock
    private FilterChain filterChain;

    private RateLimitFilter filter;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    private final UUID merchantId = UUID.randomUUID();
    private final UUID clientId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        filter = new RateLimitFilter(rateLimiter, merchantRepository, rateLimitEventRepository);
        request = new MockHttpServletRequest("GET", "/v1/payments");
        response = new MockHttpServletResponse();
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private Merchant buildMerchant(RateLimitTier tier) {
        return Merchant.builder()
                .merchantId(merchantId)
                .externalId(UUID.randomUUID())
                .name("Test")
                .country("US")
                .scopes(List.of())
                .corridors(List.of())
                .status(MerchantStatus.ACTIVE)
                .rateLimitTier(tier)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .version(0)
                .build();
    }

    private void setAuthentication() {
        SecurityContextHolder.getContext().setAuthentication(
                new MerchantAuthentication(merchantId, clientId, List.of(), MerchantAuthentication.AuthMethod.JWT));
    }

    @Test
    void shouldPassThroughWhenNotAuthenticated() throws ServletException, IOException {
        filter.doFilterInternal(request, response, filterChain);

        then(filterChain).should().doFilter(request, response);
        then(rateLimiter).shouldHaveNoInteractions();
    }

    @Test
    void shouldRejectWhenMerchantNotFound() throws ServletException, IOException {
        setAuthentication();
        given(merchantRepository.findById(merchantId)).willReturn(Optional.empty());

        filter.doFilterInternal(request, response, filterChain);

        then(filterChain).should(never()).doFilter(request, response);
        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentAsString()).contains("GW-1001");
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void shouldSkipRateLimitingForUnlimitedTier() throws ServletException, IOException {
        setAuthentication();
        given(merchantRepository.findById(merchantId))
                .willReturn(Optional.of(buildMerchant(RateLimitTier.UNLIMITED)));

        filter.doFilterInternal(request, response, filterChain);

        then(filterChain).should().doFilter(request, response);
        then(rateLimiter).shouldHaveNoInteractions();
    }

    @Nested
    class PathNormalization {

        @Test
        void shouldReplaceUuidsWithPlaceholder() {
            var result = RateLimitFilter.normalizePath(
                    "/v1/merchants/550e8400-e29b-41d4-a716-446655440000/payments");
            assertThat(result).isEqualTo("/v1/merchants/{id}/payments");
        }

        @Test
        void shouldReplaceNumericIdsWithPlaceholder() {
            var result = RateLimitFilter.normalizePath("/v1/payments/12345");
            assertThat(result).isEqualTo("/v1/payments/{id}");
        }

        @Test
        void shouldPreserveNonIdSegments() {
            var result = RateLimitFilter.normalizePath("/v1/payments");
            assertThat(result).isEqualTo("/v1/payments");
        }
    }

    @Nested
    class WhenWithinLimits {

        @Test
        void shouldAllowRequestAndSetHeaders() throws ServletException, IOException {
            setAuthentication();
            var merchant = buildMerchant(RateLimitTier.STARTER);
            var endpoint = "GET /v1/payments";
            var policy = new RateLimitPolicy(RateLimitTier.STARTER);
            given(merchantRepository.findById(merchantId)).willReturn(Optional.of(merchant));
            given(rateLimiter.check(merchantId, endpoint, policy))
                    .willReturn(new RateLimitResult(true, 5, 60, "1m", 0));

            filter.doFilterInternal(request, response, filterChain);

            then(filterChain).should().doFilter(request, response);
            assertThat(response.getHeader("X-RateLimit-Limit")).isEqualTo("60");
            assertThat(response.getHeader("X-RateLimit-Remaining")).isEqualTo("55");
        }
    }

    @Nested
    class WhenExceedingLimits {

        @Test
        void shouldRejectWithMinuteBreachAndRetryAfter() throws ServletException, IOException {
            setAuthentication();
            var merchant = buildMerchant(RateLimitTier.STARTER);
            var endpoint = "GET /v1/payments";
            var policy = new RateLimitPolicy(RateLimitTier.STARTER);
            given(merchantRepository.findById(merchantId)).willReturn(Optional.of(merchant));
            given(rateLimiter.check(merchantId, endpoint, policy))
                    .willReturn(new RateLimitResult(false, 61, 60, "1m", 60));

            filter.doFilterInternal(request, response, filterChain);

            then(filterChain).should(never()).doFilter(request, response);
            assertThat(response.getStatus()).isEqualTo(429);
            assertThat(response.getHeader("Retry-After")).isEqualTo("60");
            assertThat(response.getContentAsString()).contains("GW-6001");
        }

        @Test
        void shouldRejectWithDayBreachAndRetryAfter() throws ServletException, IOException {
            setAuthentication();
            var merchant = buildMerchant(RateLimitTier.STARTER);
            var endpoint = "GET /v1/payments";
            var policy = new RateLimitPolicy(RateLimitTier.STARTER);
            given(merchantRepository.findById(merchantId)).willReturn(Optional.of(merchant));
            given(rateLimiter.check(merchantId, endpoint, policy))
                    .willReturn(new RateLimitResult(false, 10001, 10000, "1d", 3600));

            filter.doFilterInternal(request, response, filterChain);

            then(filterChain).should(never()).doFilter(request, response);
            assertThat(response.getStatus()).isEqualTo(429);
            assertThat(response.getHeader("Retry-After")).isEqualTo("3600");
        }

        @Test
        void shouldPersistRateLimitEvent() throws ServletException, IOException {
            setAuthentication();
            var merchant = buildMerchant(RateLimitTier.STARTER);
            var endpoint = "GET /v1/payments";
            var policy = new RateLimitPolicy(RateLimitTier.STARTER);
            given(merchantRepository.findById(merchantId)).willReturn(Optional.of(merchant));
            given(rateLimiter.check(merchantId, endpoint, policy))
                    .willReturn(new RateLimitResult(false, 61, 60, "1m", 60));

            filter.doFilterInternal(request, response, filterChain);

            var expectedEvent = RateLimitEvent.builder()
                    .merchantId(merchantId)
                    .endpoint(endpoint)
                    .tier(RateLimitTier.STARTER)
                    .requestCount(61)
                    .limitValue(60)
                    .breached(true)
                    .build();
            then(rateLimitEventRepository).should().save(eqIgnoring(expectedEvent, "eventId", "occurredAt"));
        }
    }
}
