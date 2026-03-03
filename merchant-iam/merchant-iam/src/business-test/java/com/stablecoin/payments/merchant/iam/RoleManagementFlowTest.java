package com.stablecoin.payments.merchant.iam;

import com.jayway.jsonpath.JsonPath;
import com.stablecoin.payments.merchant.iam.infrastructure.persistence.repository.RoleJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Tag("business")
@DisplayName("Role Management Flow")
class RoleManagementFlowTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RoleJpaRepository roleJpa;

    @Autowired
    private JdbcTemplate jdbc;

    private UUID merchantId;

    @BeforeEach
    void seedBuiltInRoles() {
        merchantId = UUID.randomUUID();

        jdbc.update("""
            INSERT INTO roles (role_id, merchant_id, role_name, description, is_builtin, is_active, created_at, updated_at)
            VALUES (?, ?, 'ADMIN', 'Administrator', true, true, NOW(), NOW()),
                   (?, ?, 'VIEWER', 'Viewer', true, true, NOW(), NOW()),
                   (?, ?, 'PAYMENTS_OPERATOR', 'Payments Operator', true, true, NOW(), NOW()),
                   (?, ?, 'DEVELOPER', 'Developer', true, true, NOW(), NOW())
            """,
                UUID.randomUUID(), merchantId,
                UUID.randomUUID(), merchantId,
                UUID.randomUUID(), merchantId,
                UUID.randomUUID(), merchantId);

        var adminRoleId = roleJpa.findByMerchantIdAndRoleName(merchantId, "ADMIN").orElseThrow().getRoleId();
        jdbc.update("INSERT INTO role_permissions (role_permission_id, role_id, permission, created_at) VALUES (?, ?, '*:*', NOW())",
                UUID.randomUUID(), adminRoleId);

        jdbc.update("""
            INSERT INTO merchant_users (user_id, merchant_id, email, email_hash, full_name, status,
                role_id, mfa_enabled, auth_provider, created_at, updated_at, activated_at, version)
            VALUES (?, ?, ?, 'admin-hash-rm', 'Admin', 'ACTIVE',
                ?, false, 'LOCAL', NOW(), NOW(), NOW(), 0)
            """, UUID.randomUUID(), merchantId, "admin@acme.com".getBytes(), adminRoleId);
    }

    @Test
    @DisplayName("should list built-in roles")
    void shouldListBuiltInRoles() throws Exception {
        mockMvc.perform(get("/v1/merchants/{id}/roles", merchantId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()", is(4)))
                .andExpect(jsonPath("$.data[*].roleName",
                        hasItems("ADMIN", "VIEWER", "PAYMENTS_OPERATOR", "DEVELOPER")));
    }

    @Test
    @DisplayName("should create custom role and retrieve it")
    void shouldCreateCustomRole() throws Exception {
        // given / when
        var createResult = mockMvc.perform(post("/v1/merchants/{id}/roles", merchantId)
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "roleName": "FINANCE_AUDITOR",
                                    "description": "Read-only finance access",
                                    "permissions": ["transactions:read", "exports:read", "compliance:read"]
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.roleName", is("FINANCE_AUDITOR")))
                .andExpect(jsonPath("$.data.builtin", is(false)))
                .andExpect(jsonPath("$.data.roleId", notNullValue()))
                .andReturn();

        var roleId = JsonPath.read(
                createResult.getResponse().getContentAsString(), "$.data.roleId").toString();

        // then — DB reflects new role
        var createdRole = roleJpa.findById(UUID.fromString(roleId)).orElseThrow();
        assertThat(createdRole).extracting("roleName", "builtin", "active")
                .containsExactly("FINANCE_AUDITOR", false, true);
        var permCount = jdbc.queryForObject(
                "SELECT count(*) FROM role_permissions WHERE role_id = ?",
                Integer.class, UUID.fromString(roleId));
        assertThat(permCount).isEqualTo(3);
    }

    @Test
    @DisplayName("should create custom role then update its permissions")
    void shouldUpdateCustomRolePermissions() throws Exception {
        // given — create custom role
        var createResult = mockMvc.perform(post("/v1/merchants/{id}/roles", merchantId)
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "roleName": "TEMP_ROLE",
                                    "description": "Temporary role",
                                    "permissions": ["payments:read"]
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn();

        var roleId = JsonPath.read(
                createResult.getResponse().getContentAsString(), "$.data.roleId").toString();

        // when — update permissions
        mockMvc.perform(patch("/v1/merchants/{id}/roles/{roleId}", merchantId, roleId)
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "permissions": ["payments:read", "transactions:read", "exports:read"]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.permissions.length()", is(3)));

        // then — DB reflects updated permissions
        var updatedPermCount = jdbc.queryForObject(
                "SELECT count(*) FROM role_permissions WHERE role_id = ?",
                Integer.class, UUID.fromString(roleId));
        assertThat(updatedPermCount).isEqualTo(3);
    }

    @Test
    @DisplayName("should reject modifying built-in role permissions")
    void shouldRejectModifyingBuiltInRole() throws Exception {
        // given
        var adminRoleId = roleJpa.findByMerchantIdAndRoleName(merchantId, "ADMIN")
                .orElseThrow().getRoleId();

        // when / then
        mockMvc.perform(patch("/v1/merchants/{id}/roles/{roleId}", merchantId, adminRoleId)
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"permissions\": [\"payments:read\"]}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("should create then delete custom role with no users")
    void shouldDeleteCustomRoleWithNoUsers() throws Exception {
        // given — create a custom role
        var createResult = mockMvc.perform(post("/v1/merchants/{id}/roles", merchantId)
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "roleName": "DISPOSABLE_ROLE",
                                    "description": "To be deleted",
                                    "permissions": ["roles:read"]
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn();

        var roleId = JsonPath.read(
                createResult.getResponse().getContentAsString(), "$.data.roleId").toString();

        // when — delete
        mockMvc.perform(delete("/v1/merchants/{id}/roles/{roleId}", merchantId, roleId)
                        .header("Idempotency-Key", UUID.randomUUID().toString()))
                .andExpect(status().isNoContent());

        // then — role marked inactive in DB
        var deletedRole = roleJpa.findById(UUID.fromString(roleId)).orElseThrow();
        assertThat(deletedRole.isActive()).isFalse();
    }

    @Test
    @DisplayName("should reject deleting built-in role")
    void shouldRejectDeletingBuiltInRole() throws Exception {
        // given
        var viewerRoleId = roleJpa.findByMerchantIdAndRoleName(merchantId, "VIEWER")
                .orElseThrow().getRoleId();

        // when / then
        mockMvc.perform(delete("/v1/merchants/{id}/roles/{roleId}", merchantId, viewerRoleId)
                        .header("Idempotency-Key", UUID.randomUUID().toString()))
                .andExpect(status().isForbidden());
    }
}
