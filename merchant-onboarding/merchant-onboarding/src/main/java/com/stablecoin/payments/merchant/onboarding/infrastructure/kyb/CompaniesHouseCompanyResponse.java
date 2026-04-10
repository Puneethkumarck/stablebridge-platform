package com.stablecoin.payments.merchant.onboarding.infrastructure.kyb;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
record CompaniesHouseCompanyResponse(
    @JsonProperty("company_name") String companyName,
    @JsonProperty("company_number") String companyNumber,
    @JsonProperty("company_status") String companyStatus,
    @JsonProperty("type") String type,
    @JsonProperty("date_of_creation") String dateOfCreation,
    @JsonProperty("registered_office_address") CompaniesHouseAddress registeredOfficeAddress,
    @JsonProperty("sic_codes") List<String> sicCodes) {
}
