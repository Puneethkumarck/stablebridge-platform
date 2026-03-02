package com.stablecoin.payments.merchant.onboarding.domain.statemachine;

public record StateTransition<S, T>(S from, T trigger, S to) {}
