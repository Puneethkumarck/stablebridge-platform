package com.stablecoin.payments.platform.infrastructure.security;

import jakarta.servlet.ServletException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static com.stablecoin.payments.platform.infrastructure.security.SecurityHeadersFilter.HEADER_CACHE_CONTROL;
import static com.stablecoin.payments.platform.infrastructure.security.SecurityHeadersFilter.HEADER_CONTENT_SECURITY_POLICY;
import static com.stablecoin.payments.platform.infrastructure.security.SecurityHeadersFilter.HEADER_PERMISSIONS_POLICY;
import static com.stablecoin.payments.platform.infrastructure.security.SecurityHeadersFilter.HEADER_PRAGMA;
import static com.stablecoin.payments.platform.infrastructure.security.SecurityHeadersFilter.HEADER_REFERRER_POLICY;
import static com.stablecoin.payments.platform.infrastructure.security.SecurityHeadersFilter.HEADER_STRICT_TRANSPORT_SECURITY;
import static com.stablecoin.payments.platform.infrastructure.security.SecurityHeadersFilter.HEADER_X_CONTENT_TYPE_OPTIONS;
import static com.stablecoin.payments.platform.infrastructure.security.SecurityHeadersFilter.HEADER_X_FRAME_OPTIONS;
import static com.stablecoin.payments.platform.infrastructure.security.SecurityHeadersFilter.HEADER_X_XSS_PROTECTION;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SecurityHeadersFilter")
class SecurityHeadersFilterTest {

    @Test
    @DisplayName("should add all security headers when enabled with defaults")
    void shouldAddAllSecurityHeadersWhenEnabledWithDefaults() throws ServletException, IOException {
        // given
        var properties = new SecurityHeadersProperties(true, true, 31_536_000L);
        var filter = new SecurityHeadersFilter(properties);
        var request = new MockHttpServletRequest();
        var response = new MockHttpServletResponse();
        var filterChain = new MockFilterChain();

        // when
        filter.doFilter(request, response, filterChain);

        // then
        assertThat(response.getHeaderNames())
                .containsExactlyInAnyOrder(
                        HEADER_X_CONTENT_TYPE_OPTIONS,
                        HEADER_X_FRAME_OPTIONS,
                        HEADER_CONTENT_SECURITY_POLICY,
                        HEADER_X_XSS_PROTECTION,
                        HEADER_CACHE_CONTROL,
                        HEADER_PRAGMA,
                        HEADER_REFERRER_POLICY,
                        HEADER_PERMISSIONS_POLICY,
                        HEADER_STRICT_TRANSPORT_SECURITY
                );

        assertThat(response.getHeader(HEADER_X_CONTENT_TYPE_OPTIONS)).isEqualTo("nosniff");
        assertThat(response.getHeader(HEADER_X_FRAME_OPTIONS)).isEqualTo("DENY");
        assertThat(response.getHeader(HEADER_CONTENT_SECURITY_POLICY)).isEqualTo("default-src 'none'");
        assertThat(response.getHeader(HEADER_X_XSS_PROTECTION)).isEqualTo("0");
        assertThat(response.getHeader(HEADER_CACHE_CONTROL)).isEqualTo("no-store");
        assertThat(response.getHeader(HEADER_PRAGMA)).isEqualTo("no-cache");
        assertThat(response.getHeader(HEADER_REFERRER_POLICY)).isEqualTo("strict-origin-when-cross-origin");
        assertThat(response.getHeader(HEADER_PERMISSIONS_POLICY)).isEqualTo("camera=(), microphone=(), geolocation=()");
        assertThat(response.getHeader(HEADER_STRICT_TRANSPORT_SECURITY)).isEqualTo("max-age=31536000; includeSubDomains");
    }

    @Test
    @DisplayName("should not add HSTS header when hsts-enabled is false")
    void shouldNotAddHstsHeaderWhenHstsDisabled() throws ServletException, IOException {
        // given
        var properties = new SecurityHeadersProperties(true, false, 31_536_000L);
        var filter = new SecurityHeadersFilter(properties);
        var request = new MockHttpServletRequest();
        var response = new MockHttpServletResponse();
        var filterChain = new MockFilterChain();

        // when
        filter.doFilter(request, response, filterChain);

        // then
        assertThat(response.getHeaderNames()).doesNotContain(HEADER_STRICT_TRANSPORT_SECURITY);
        assertThat(response.getHeader(HEADER_X_CONTENT_TYPE_OPTIONS)).isEqualTo("nosniff");
        assertThat(response.getHeader(HEADER_X_FRAME_OPTIONS)).isEqualTo("DENY");
    }

    @Test
    @DisplayName("should use custom HSTS max-age when configured")
    void shouldUseCustomHstsMaxAge() throws ServletException, IOException {
        // given
        var customMaxAge = 86_400L;
        var properties = new SecurityHeadersProperties(true, true, customMaxAge);
        var filter = new SecurityHeadersFilter(properties);
        var request = new MockHttpServletRequest();
        var response = new MockHttpServletResponse();
        var filterChain = new MockFilterChain();

        // when
        filter.doFilter(request, response, filterChain);

        // then
        assertThat(response.getHeader(HEADER_STRICT_TRANSPORT_SECURITY)).isEqualTo("max-age=86400; includeSubDomains");
    }

    @Test
    @DisplayName("should continue filter chain after adding headers")
    void shouldContinueFilterChainAfterAddingHeaders() throws ServletException, IOException {
        // given
        var properties = new SecurityHeadersProperties(true, true, 31_536_000L);
        var filter = new SecurityHeadersFilter(properties);
        var request = new MockHttpServletRequest();
        var response = new MockHttpServletResponse();
        var filterChain = new MockFilterChain();

        // when
        filter.doFilter(request, response, filterChain);

        // then
        assertThat(filterChain.getRequest()).isEqualTo(request);
        assertThat(filterChain.getResponse()).isEqualTo(response);
    }

    @Test
    @DisplayName("should apply default property values when nulls provided")
    void shouldApplyDefaultPropertyValuesWhenNullsProvided() throws ServletException, IOException {
        // given
        var properties = new SecurityHeadersProperties(null, null, null);
        var filter = new SecurityHeadersFilter(properties);
        var request = new MockHttpServletRequest();
        var response = new MockHttpServletResponse();
        var filterChain = new MockFilterChain();

        // when
        filter.doFilter(request, response, filterChain);

        // then
        assertThat(response.getHeader(HEADER_STRICT_TRANSPORT_SECURITY)).isEqualTo("max-age=31536000; includeSubDomains");
        assertThat(response.getHeader(HEADER_X_CONTENT_TYPE_OPTIONS)).isEqualTo("nosniff");
    }
}
