package com.stablecoin.payments.merchant.onboarding.domain;

public interface EventPublisher<T> {
    void publish(T event);
}
