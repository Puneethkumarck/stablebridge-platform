package com.stablecoin.payments.merchant.onboarding.infrastructure.persistence;

import com.stablecoin.payments.merchant.onboarding.AbstractIntegrationTest;
import com.stablecoin.payments.merchant.onboarding.fixtures.MerchantEntityFixtures;
import com.stablecoin.payments.merchant.onboarding.infrastructure.persistence.entity.ApprovedCorridorJpaRepository;
import com.stablecoin.payments.merchant.onboarding.infrastructure.persistence.entity.MerchantJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ApprovedCorridorRepositoryAdapter IT")
class ApprovedCorridorRepositoryAdapterIT extends AbstractIntegrationTest {

    @Autowired
    private ApprovedCorridorJpaRepository corridorJpa;

    @Autowired
    private MerchantJpaRepository merchantJpa;

    @BeforeEach
    void cleanUp() {
        corridorJpa.deleteAll();
        merchantJpa.deleteAll();
    }

    @Test
    @DisplayName("should save and find corridor by merchant ID")
    @Transactional
    void shouldSaveAndFindByMerchantId() {
        // given
        var merchant = MerchantEntityFixtures.anActiveMerchantEntity();
        merchantJpa.save(merchant);
        var corridor = MerchantEntityFixtures.anApprovedCorridorEntity(merchant.getMerchantId());

        // when
        corridorJpa.save(corridor);
        var found = corridorJpa.findByMerchantId(merchant.getMerchantId());

        // then
        assertThat(found).hasSize(1);
        assertThat(found.getFirst().getSourceCountry()).isEqualTo("GB");
        assertThat(found.getFirst().getTargetCountry()).isEqualTo("US");
        assertThat(found.getFirst().getCurrencies()).containsExactly("GBP", "USD");
    }

    @Test
    @DisplayName("should check existence by merchant and country pair")
    @Transactional
    void shouldCheckExistenceByMerchantAndCountry() {
        // given
        var merchant = MerchantEntityFixtures.anActiveMerchantEntity();
        merchantJpa.save(merchant);
        var corridor = MerchantEntityFixtures.anApprovedCorridorEntity(merchant.getMerchantId());
        corridorJpa.save(corridor);

        // when / then
        assertThat(corridorJpa.existsByMerchantIdAndSourceCountryAndTargetCountry(
                merchant.getMerchantId(), "GB", "US")).isTrue();
        assertThat(corridorJpa.existsByMerchantIdAndSourceCountryAndTargetCountry(
                merchant.getMerchantId(), "US", "DE")).isFalse();
    }

    @Test
    @DisplayName("should return empty list for unknown merchant")
    void shouldReturnEmptyForUnknownMerchant() {
        var found = corridorJpa.findByMerchantId(UUID.randomUUID());
        assertThat(found).isEmpty();
    }
}
