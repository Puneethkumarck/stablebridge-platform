package com.stablecoin.payments.fx.infrastructure.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@RequiredArgsConstructor
public class FxMetrics {

    private final MeterRegistry meterRegistry;
    private final Map<String, AtomicInteger> activeLockCounters = new ConcurrentHashMap<>();

    public void incrementActiveLocks(String corridor) {
        getActiveLockCounter(corridor).incrementAndGet();
    }

    public void decrementActiveLocks(String corridor) {
        getActiveLockCounter(corridor).decrementAndGet();
    }

    public void recordQuoteCreated(String corridor) {
        meterRegistry.counter("fx.quote.created",
                "corridor", corridor
        ).increment();
    }

    public void recordLockAcquired(String corridor) {
        meterRegistry.counter("fx.lock.acquired",
                "corridor", corridor
        ).increment();
    }

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
