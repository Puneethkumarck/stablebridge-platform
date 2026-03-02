package com.stablecoin.payments.merchant.onboarding.domain.merchant.model.core;

import lombok.Builder;

@Builder(toBuilder = true)
public record BusinessAddress(
        String streetLine1,
        String streetLine2,
        String city,
        String stateProvince,
        String postcode,
        String country
) {}
