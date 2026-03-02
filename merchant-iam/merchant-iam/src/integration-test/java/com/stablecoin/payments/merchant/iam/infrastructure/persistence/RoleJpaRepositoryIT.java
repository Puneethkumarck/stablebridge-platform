package com.stablecoin.payments.merchant.iam.infrastructure.persistence;

import com.stablecoin.payments.merchant.iam.AbstractIntegrationTest;
import com.stablecoin.payments.merchant.iam.fixtures.IamEntityFixtures;
import com.stablecoin.payments.merchant.iam.infrastructure.persistence.repository.RoleJpaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RoleJpaRepository IT")
class RoleJpaRepositoryIT extends AbstractIntegrationTest {

    @Autowired
    private RoleJpaRepository roleJpaRepository;

    @Test
    @DisplayName("should save and find role by id")
    void shouldSaveAndFindById() {
        var role = IamEntityFixtures.anAdminRole();
        roleJpaRepository.save(role);

        var found = roleJpaRepository.findById(role.getRoleId());

        assertThat(found).isPresent();
        assertThat(found.get().getRoleName()).isEqualTo("ADMIN");
        assertThat(found.get().isBuiltin()).isTrue();
        assertThat(found.get().isActive()).isTrue();
    }

    @Test
    @DisplayName("should find active roles by merchant id")
    void shouldFindActiveRolesByMerchantId() {
        var admin = IamEntityFixtures.anAdminRole();
        var viewer = IamEntityFixtures.aViewerRole();
        var inactive = IamEntityFixtures.anInactiveRole();
        roleJpaRepository.save(admin);
        roleJpaRepository.save(viewer);
        roleJpaRepository.save(inactive);

        var activeRoles = roleJpaRepository.findByMerchantIdAndActiveTrue(IamEntityFixtures.defaultMerchantId());

        assertThat(activeRoles).hasSize(2);
        assertThat(activeRoles).extracting("roleName").containsExactlyInAnyOrder("ADMIN", "VIEWER");
    }

    @Test
    @DisplayName("should find role by merchant id and role name")
    void shouldFindByMerchantIdAndRoleName() {
        var role = IamEntityFixtures.anAdminRole();
        roleJpaRepository.save(role);

        var found = roleJpaRepository.findByMerchantIdAndRoleName(IamEntityFixtures.defaultMerchantId(), "ADMIN");

        assertThat(found).isPresent();
        assertThat(found.get().getDescription()).isEqualTo("Full access");
    }

    @Test
    @DisplayName("should check existence by merchant id and role name")
    void shouldCheckExistenceByMerchantIdAndRoleName() {
        var role = IamEntityFixtures.anAdminRole();
        roleJpaRepository.save(role);

        assertThat(roleJpaRepository.existsByMerchantIdAndRoleName(IamEntityFixtures.defaultMerchantId(), "ADMIN"))
                .isTrue();
        assertThat(roleJpaRepository.existsByMerchantIdAndRoleName(IamEntityFixtures.defaultMerchantId(), "NONEXISTENT"))
                .isFalse();
    }
}
