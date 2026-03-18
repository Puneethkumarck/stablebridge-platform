package com.stablecoin.payments.merchant.onboarding.infrastructure.kyb;

import com.stablecoin.payments.merchant.onboarding.domain.merchant.CompanyRegistryProvider.CompanyProfile;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sandbox tests that call the real Companies House API. Requires a valid {@code COMPANIES_HOUSE_API_KEY} environment
 * variable. Excluded from normal CI builds — run manually with:
 *
 * <pre>
 * COMPANIES_HOUSE_API_KEY=xxx ./gradlew :merchant-onboarding:merchant-onboarding:test --tests "*CompaniesHouseAdapterSandboxTest"
 * </pre>
 */
@Tag("sandbox")
@DisplayName("CompaniesHouseAdapter (sandbox)")
@EnabledIfEnvironmentVariable(named = "COMPANIES_HOUSE_API_KEY", matches = ".+")
class CompaniesHouseAdapterSandboxTest {

    private static CompaniesHouseAdapter adapter;

    @BeforeAll
    static void setUp() {
        var apiKey = System.getenv("COMPANIES_HOUSE_API_KEY");
        var properties = new CompaniesHouseProperties(
                "https://api.company-information.service.gov.uk", apiKey, 5000, 10000);
        adapter = new CompaniesHouseAdapter(properties);
    }

    @Test
    @DisplayName("should look up a known UK company (00000006)")
    void shouldLookUpKnownCompany() {
        var result = adapter.lookup("00000006", "GB");

        assertThat(result).isPresent();
        var expected = new CompanyProfile(
                "MARINE AND GENERAL MUTUAL LIFE ASSURANCE SOCIETY",
                "00000006",
                "GB",
                "active",
                "private-unlimited-nsc",
                "1862-10-25",
                null);
        assertThat(result.get()).usingRecursiveComparison()
                .ignoringFields("registeredOfficeAddress")
                .isEqualTo(expected);
    }

    @Test
    @DisplayName("should return empty for non-existent company number")
    void shouldReturnEmptyForNonExistentCompany() {
        var result = adapter.lookup("99999999", "GB");

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("should return empty for non-GB country")
    void shouldReturnEmptyForNonGbCountry() {
        var result = adapter.lookup("00000006", "US");

        assertThat(result).isEmpty();
    }
}
