package com.stablecoin.payments.offramp.domain.service;

import com.stablecoin.payments.offramp.domain.model.PayoutOrder;

public record PayoutResult(PayoutOrder order, boolean created) {}
