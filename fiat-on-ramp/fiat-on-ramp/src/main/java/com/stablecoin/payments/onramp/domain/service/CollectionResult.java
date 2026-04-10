package com.stablecoin.payments.onramp.domain.service;

import com.stablecoin.payments.onramp.domain.model.CollectionOrder;

public record CollectionResult(CollectionOrder order, boolean created) {}
