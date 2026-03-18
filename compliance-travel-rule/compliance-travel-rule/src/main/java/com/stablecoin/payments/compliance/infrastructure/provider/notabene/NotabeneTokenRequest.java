package com.stablecoin.payments.compliance.infrastructure.provider.notabene;

import com.fasterxml.jackson.annotation.JsonProperty;

record NotabeneTokenRequest(
        @JsonProperty("client_id") String clientId,
        @JsonProperty("client_secret") String clientSecret,
        @JsonProperty("grant_type") String grantType,
        String audience
) {}
