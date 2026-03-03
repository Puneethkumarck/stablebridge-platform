package com.stablecoin.payments.merchant.iam.application.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Registers the 11 Micrometer counters specified in the S13 spec Section 7.
 * Inject the {@link Counter} beans where needed, or use {@link MeterRegistry} directly.
 */
@Configuration
public class MetricsConfig {

    @Bean
    public Counter loginSuccessCounter(MeterRegistry registry) {
        return Counter.builder("iam.login.success")
                .description("Successful login attempts")
                .register(registry);
    }

    @Bean
    public Counter loginFailureCounter(MeterRegistry registry) {
        return Counter.builder("iam.login.failure")
                .description("Failed login attempts (wrong credentials)")
                .register(registry);
    }

    @Bean
    public Counter loginLockedOutCounter(MeterRegistry registry) {
        return Counter.builder("iam.login.locked_out")
                .description("Login attempts blocked by brute-force lockout")
                .register(registry);
    }

    @Bean
    public Counter inviteSentCounter(MeterRegistry registry) {
        return Counter.builder("iam.invite.sent")
                .description("User invitations sent")
                .register(registry);
    }

    @Bean
    public Counter invitationAcceptedCounter(MeterRegistry registry) {
        return Counter.builder("iam.invite.accepted")
                .description("User invitations accepted")
                .register(registry);
    }

    @Bean
    public Counter userSuspendedCounter(MeterRegistry registry) {
        return Counter.builder("iam.user.suspended")
                .description("Users suspended")
                .register(registry);
    }

    @Bean
    public Counter userDeactivatedCounter(MeterRegistry registry) {
        return Counter.builder("iam.user.deactivated")
                .description("Users deactivated")
                .register(registry);
    }

    @Bean
    public Counter roleChangedCounter(MeterRegistry registry) {
        return Counter.builder("iam.user.role_changed")
                .description("User role changes")
                .register(registry);
    }

    @Bean
    public Counter permissionCacheHitCounter(MeterRegistry registry) {
        return Counter.builder("iam.permission_cache.hit")
                .description("Permission cache hits")
                .register(registry);
    }

    @Bean
    public Counter permissionCacheMissCounter(MeterRegistry registry) {
        return Counter.builder("iam.permission_cache.miss")
                .description("Permission cache misses (DB fallback)")
                .register(registry);
    }

    @Bean
    public Counter mfaVerifyFailureCounter(MeterRegistry registry) {
        return Counter.builder("iam.mfa.verify_failure")
                .description("Failed MFA TOTP verification attempts")
                .register(registry);
    }
}
