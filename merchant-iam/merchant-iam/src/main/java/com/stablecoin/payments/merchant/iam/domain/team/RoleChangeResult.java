package com.stablecoin.payments.merchant.iam.domain.team;

import com.stablecoin.payments.merchant.iam.domain.team.model.MerchantUser;

import java.util.UUID;

public record RoleChangeResult(
        MerchantUser user,
        String previousRoleName,
        String newRoleName,
        UUID changedBy
) {}
