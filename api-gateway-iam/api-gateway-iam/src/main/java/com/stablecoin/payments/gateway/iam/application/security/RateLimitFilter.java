package com.stablecoin.payments.gateway.iam.application.security;

import com.stablecoin.payments.gateway.iam.domain.model.RateLimitEvent;
import com.stablecoin.payments.gateway.iam.domain.model.RateLimitTier;
import com.stablecoin.payments.gateway.iam.domain.port.MerchantRepository;
import com.stablecoin.payments.gateway.iam.domain.port.RateLimitEventRepository;
import com.stablecoin.payments.gateway.iam.domain.port.RateLimiter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {

    private final RateLimiter rateLimiter;
    private final MerchantRepository merchantRepository;
    private final RateLimitEventRepository rateLimitEventRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        var auth = SecurityContextHolder.getContext().getAuthentication();

        if (!(auth instanceof MerchantAuthentication merchantAuth)) {
            chain.doFilter(request, response);
            return;
        }

        var merchantId = merchantAuth.merchantId();
        var endpoint = request.getMethod() + " " + request.getRequestURI();

        var merchant = merchantRepository.findById(merchantId).orElse(null);
        if (merchant == null) {
            chain.doFilter(request, response);
            return;
        }

        var policy = merchant.rateLimitPolicy();
        if (policy.isUnlimited()) {
            chain.doFilter(request, response);
            return;
        }

        var result = rateLimiter.check(merchantId, endpoint, policy);

        response.setHeader("X-RateLimit-Limit", String.valueOf(result.limit()));
        response.setHeader("X-RateLimit-Remaining",
                String.valueOf(Math.max(0, result.limit() - result.currentCount())));

        if (!result.allowed()) {
            var retryAfter = "1m".equals(result.window()) ? 60 : 3600;
            response.setHeader("Retry-After", String.valueOf(retryAfter));

            persistRateLimitEvent(merchantId, endpoint, merchant.getRateLimitTier(),
                    result.currentCount(), result.limit());

            log.warn("Rate limit exceeded merchantId={} endpoint={} count={}/{}",
                    merchantId, endpoint, result.currentCount(), result.limit());

            response.setStatus(429);
            response.setContentType("application/json");
            response.getWriter().write(
                    "{\"code\":\"GW-6001\",\"status\":\"Too Many Requests\","
                            + "\"message\":\"Rate limit exceeded. Retry after %d seconds\"}".formatted(retryAfter)
            );
            return;
        }

        chain.doFilter(request, response);
    }

    private void persistRateLimitEvent(UUID merchantId, String endpoint, RateLimitTier tier,
                                       int requestCount, int limitValue) {
        try {
            rateLimitEventRepository.save(RateLimitEvent.builder()
                    .eventId(UUID.randomUUID())
                    .merchantId(merchantId)
                    .endpoint(endpoint)
                    .tier(tier)
                    .requestCount(requestCount)
                    .limitValue(limitValue)
                    .breached(true)
                    .occurredAt(Instant.now())
                    .build());
        } catch (Exception e) {
            log.error("Failed to persist rate limit event for merchantId={}: {}", merchantId, e.getMessage());
        }
    }
}
