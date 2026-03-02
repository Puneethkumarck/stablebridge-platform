package com.stablecoin.payments.merchant.onboarding.infrastructure.persistence.entity;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface MerchantJpaRepository extends JpaRepository<MerchantEntity, UUID> {

    Optional<MerchantEntity> findByRegistrationNumberAndRegistrationCountry(
            String registrationNumber, String registrationCountry);

    boolean existsByRegistrationNumberAndRegistrationCountry(
            String registrationNumber, String registrationCountry);
}
