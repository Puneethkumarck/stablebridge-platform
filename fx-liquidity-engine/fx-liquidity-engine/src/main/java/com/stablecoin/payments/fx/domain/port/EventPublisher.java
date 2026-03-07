package com.stablecoin.payments.fx.domain.port;

public interface EventPublisher<T> {
    void publish(T event);
}
