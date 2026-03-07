package com.stablecoin.payments.fx.domain.statemachine;

public record StateTransition<S, T>(S from, T trigger, S to) {}
