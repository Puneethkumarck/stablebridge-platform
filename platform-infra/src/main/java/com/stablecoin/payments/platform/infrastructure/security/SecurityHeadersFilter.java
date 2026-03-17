package com.stablecoin.payments.platform.infrastructure.security;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
@ConditionalOnProperty(name = "app.security.headers.enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(SecurityHeadersProperties.class)
public class SecurityHeadersFilter implements Filter {

    static final String HEADER_X_CONTENT_TYPE_OPTIONS = "X-Content-Type-Options";
    static final String HEADER_X_FRAME_OPTIONS = "X-Frame-Options";
    static final String HEADER_CONTENT_SECURITY_POLICY = "Content-Security-Policy";
    static final String HEADER_X_XSS_PROTECTION = "X-XSS-Protection";
    static final String HEADER_CACHE_CONTROL = "Cache-Control";
    static final String HEADER_PRAGMA = "Pragma";
    static final String HEADER_REFERRER_POLICY = "Referrer-Policy";
    static final String HEADER_PERMISSIONS_POLICY = "Permissions-Policy";
    static final String HEADER_STRICT_TRANSPORT_SECURITY = "Strict-Transport-Security";

    private final SecurityHeadersProperties properties;

    public SecurityHeadersFilter(SecurityHeadersProperties properties) {
        this.properties = properties;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        if (response instanceof HttpServletResponse httpResponse) {
            httpResponse.setHeader(HEADER_X_CONTENT_TYPE_OPTIONS, "nosniff");
            httpResponse.setHeader(HEADER_X_FRAME_OPTIONS, "DENY");
            httpResponse.setHeader(HEADER_CONTENT_SECURITY_POLICY, "default-src 'none'");
            httpResponse.setHeader(HEADER_X_XSS_PROTECTION, "0");
            httpResponse.setHeader(HEADER_CACHE_CONTROL, "no-store");
            httpResponse.setHeader(HEADER_PRAGMA, "no-cache");
            httpResponse.setHeader(HEADER_REFERRER_POLICY, "strict-origin-when-cross-origin");
            httpResponse.setHeader(HEADER_PERMISSIONS_POLICY, "camera=(), microphone=(), geolocation=()");

            if (properties.hstsEnabled()) {
                httpResponse.setHeader(HEADER_STRICT_TRANSPORT_SECURITY,
                        "max-age=" + properties.hstsMaxAge() + "; includeSubDomains");
            }
        }

        chain.doFilter(request, response);
    }
}
