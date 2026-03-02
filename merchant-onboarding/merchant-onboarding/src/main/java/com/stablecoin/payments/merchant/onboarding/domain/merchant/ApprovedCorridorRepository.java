package com.stablecoin.payments.merchant.onboarding.domain.merchant;

import com.stablecoin.payments.merchant.onboarding.domain.merchant.model.core.ApprovedCorridor;

import java.util.List;
import java.util.UUID;

public interface ApprovedCorridorRepository {

    ApprovedCorridor save(ApprovedCorridor corridor);

    List<ApprovedCorridor> findByMerchantId(UUID merchantId);
}
