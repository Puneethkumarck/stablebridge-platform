package com.stablecoin.payments.merchant.onboarding.domain.merchant;

import java.util.Optional;
import java.util.UUID;

public interface MerchantRepository {
    Merchant save(Merchant merchant);
    Optional<Merchant> findById(UUID merchantId);
    Optional<Merchant> findByRegistrationNumberAndCountry(String registrationNumber, String country);
    boolean existsByRegistrationNumberAndCountry(String registrationNumber, String country);
}
