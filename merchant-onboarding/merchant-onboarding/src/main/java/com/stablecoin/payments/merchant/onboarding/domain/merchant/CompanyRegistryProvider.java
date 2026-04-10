package com.stablecoin.payments.merchant.onboarding.domain.merchant;

import java.util.Optional;

public interface CompanyRegistryProvider {

  Optional<CompanyProfile> lookup(String registrationNumber, String country);

  record CompanyProfile(String companyName, String registrationNumber, String country, String companyStatus,
      String companyType, String dateOfCreation, String registeredOfficeAddress) {
  }
}
