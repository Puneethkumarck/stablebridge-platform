package com.stablecoin.payments.merchant.iam.infrastructure.persistence;

import com.stablecoin.payments.merchant.iam.AbstractIntegrationTest;
import com.stablecoin.payments.merchant.iam.fixtures.IamEntityFixtures;
import com.stablecoin.payments.merchant.iam.infrastructure.persistence.repository.RoleJpaRepository;
import com.stablecoin.payments.merchant.iam.infrastructure.persistence.repository.RolePermissionJpaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RolePermissionJpaRepository IT")
class RolePermissionJpaRepositoryIT extends AbstractIntegrationTest {

    @Autowired
    private RolePermissionJpaRepository rolePermissionJpaRepository;

    @Autowired
    private RoleJpaRepository roleJpaRepository;

    @Test
    @DisplayName("should find permissions by role id")
    void shouldFindByRoleId() {
        var role = IamEntityFixtures.anAdminRole();
        roleJpaRepository.save(role);

        var perm1 = IamEntityFixtures.aRolePermission(role, "merchant:read");
        var perm2 = IamEntityFixtures.aRolePermission(role, "merchant:write");
        rolePermissionJpaRepository.save(perm1);
        rolePermissionJpaRepository.save(perm2);

        var permissions = rolePermissionJpaRepository.findByRole_RoleId(role.getRoleId());

        assertThat(permissions).hasSize(2);
        assertThat(permissions).extracting("permission").containsExactlyInAnyOrder("merchant:read", "merchant:write");
    }

    @Test
    @Transactional
    @DisplayName("should delete permissions by role id")
    void shouldDeleteByRoleId() {
        var role = IamEntityFixtures.anAdminRole();
        roleJpaRepository.save(role);

        var perm = IamEntityFixtures.aRolePermission(role, "merchant:read");
        rolePermissionJpaRepository.save(perm);

        rolePermissionJpaRepository.deleteByRole_RoleId(role.getRoleId());

        assertThat(rolePermissionJpaRepository.findByRole_RoleId(role.getRoleId())).isEmpty();
    }
}
