package com.stablecoin.payments.merchant.onboarding.api.request;

public record UpdateMerchantRequest(
        String tradingName,
        String websiteUrl,
        MerchantApplicationRequest.BusinessAddressDto registeredAddress
) {}
