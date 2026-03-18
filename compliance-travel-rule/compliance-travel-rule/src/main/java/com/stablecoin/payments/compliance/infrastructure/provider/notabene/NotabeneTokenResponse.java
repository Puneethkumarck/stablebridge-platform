package com.stablecoin.payments.compliance.infrastructure.provider.notabene;

import com.fasterxml.jackson.annotation.JsonProperty;

record NotabeneTokenResponse(
        @JsonProperty("access_token") String accessToken,
        @JsonProperty("expires_in") Integer expiresIn,
        @JsonProperty("token_type") String tokenType
) {}
