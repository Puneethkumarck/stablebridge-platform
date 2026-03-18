package com.stablecoin.payments.fx.infrastructure.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Custom business metrics for the FX and Liquidity Engine service.
 *
 * <p>Tracks active FX lock count (gauge), quote creation, lock acquisition, and lock expiry.
 */
@Component
@RequiredArgsConstructor
public class FxMetrics {

    private final MeterRegistry meterRegistry;
    private final Map<String, AtomicInteger> activeLockCounters = new ConcurrentHashMap<>();

    /**
     * Increments the active lock gauge for a corridor.
     */
    public void incrementActiveLocks(String corridor) {
        getActiveLockCounter(corridor).incrementAndGet();
    }

    /**
     * Decrements the active lock gauge for a corridor.
     */
    public void decrementActiveLocks(String corridor) {
        getActiveLockCounter(corridor).decrementAndGet();
    }

    /**
     * Records a quote creation event.
     */
    public void recordQuoteCreated(String corridor) {
        meterRegistry.counter("fx.quote.created",
                "corridor", corridor
        ).increment();
    }

    /**
     * Records a lock acquisition event.
     */
    public void recordLockAcquired(String corridor) {
        meterRegistry.counter("fx.lock.acquired",
                "corridor", corridor
        ).increment();
    }

    /**
     * Records a lock expiry event.
     */
    public void recordLockExpired(String corridor) {
        meterRegistry.counter("fx.lock.expired",
                "corridor", corridor
        ).increment();
    }

    private AtomicInteger getActiveLockCounter(String corridor) {
        return activeLockCounters.computeIfAbsent(corridor, key -> {
            var counter = new AtomicInteger(0);
            meterRegistry.gauge("fx.lock.active.count", Tags.of("corridor", key), counter);
            return counter;
        });
    }
}
