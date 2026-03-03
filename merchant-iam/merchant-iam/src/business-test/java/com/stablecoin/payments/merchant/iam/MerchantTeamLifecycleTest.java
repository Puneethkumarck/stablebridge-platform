package com.stablecoin.payments.merchant.iam;

import com.stablecoin.payments.merchant.iam.infrastructure.persistence.repository.MerchantUserJpaRepository;
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
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Tag("business")
@DisplayName("Merchant Team Lifecycle")
class MerchantTeamLifecycleTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MerchantUserJpaRepository userJpa;

    @Autowired
    private RoleJpaRepository roleJpa;

    @Autowired
    private JdbcTemplate jdbc;

    private UUID merchantId;

    @BeforeEach
    void seedMerchantTeam() {
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

        var viewerRoleId = roleJpa.findByMerchantIdAndRoleName(merchantId, "VIEWER").orElseThrow().getRoleId();
        for (var perm : new String[]{"payments:read", "transactions:read", "roles:read", "settings:read"}) {
            jdbc.update("INSERT INTO role_permissions (role_permission_id, role_id, permission, created_at) VALUES (?, ?, ?, NOW())",
                    UUID.randomUUID(), viewerRoleId, perm);
        }

        jdbc.update("""
            INSERT INTO merchant_users (user_id, merchant_id, email, email_hash, full_name, status,
                role_id, mfa_enabled, auth_provider, created_at, updated_at, activated_at, version)
            VALUES (?, ?, ?, ?, ?, 'ACTIVE', ?, false, 'LOCAL', NOW(), NOW(), NOW(), 0)
            """,
                UUID.randomUUID(), merchantId,
                "admin@acme.com".getBytes(), "admin-hash", "Admin User", adminRoleId);
    }

    // ── Flow 1: Invite user ──────────────────────────────────────────────────

    @Test
    @DisplayName("should invite user and verify PENDING invitation created")
    void shouldInviteUserAndAcceptInvitation() throws Exception {
        // given
        var viewerRoleId = roleJpa.findByMerchantIdAndRoleName(merchantId, "VIEWER").orElseThrow().getRoleId();

        // when — invite
        mockMvc.perform(post("/v1/merchants/{id}/users/invite", merchantId)
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "email": "alice@acme.com",
                                    "fullName": "Alice Smith",
                                    "roleId": "%s"
                                }
                                """.formatted(viewerRoleId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status", is("PENDING")))
                .andExpect(jsonPath("$.data.invitationId", notNullValue()));

        // then — user created in INVITED status
        assertThat(userJpa.findByMerchantIdAndStatusNot(merchantId, "DEACTIVATED"))
                .anyMatch(u -> u.getFullName().equals("Alice Smith"));
    }

    // ── Flow 2: Full user lifecycle ──────────────────────────────────────────

    @Test
    @DisplayName("should complete user lifecycle: suspend → reactivate → deactivate")
    void shouldCompleteUserLifecycle() throws Exception {
        // given — seed a second active user (Viewer)
        var viewerRoleId = roleJpa.findByMerchantIdAndRoleName(merchantId, "VIEWER").orElseThrow().getRoleId();
        var aliceId = UUID.randomUUID();
        jdbc.update("""
            INSERT INTO merchant_users (user_id, merchant_id, email, email_hash, full_name, status,
                role_id, mfa_enabled, auth_provider, created_at, updated_at, activated_at, version)
            VALUES (?, ?, ?, ?, 'Alice', 'ACTIVE', ?, false, 'LOCAL', NOW(), NOW(), NOW(), 0)
            """, aliceId, merchantId, "alice@acme.com".getBytes(), "alice-hash-bt", viewerRoleId);

        // when — list users
        mockMvc.perform(get("/v1/merchants/{id}/users", merchantId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()", equalTo(2)));

        // when — suspend Alice
        mockMvc.perform(post("/v1/merchants/{id}/users/{uid}/suspend", merchantId, aliceId)
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\": \"policy violation\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status", is("SUSPENDED")));

        // then — DB reflects suspended
        var suspendedUser = userJpa.findById(aliceId).orElseThrow();
        assertThat(suspendedUser.getStatus()).isEqualTo("SUSPENDED");
        assertThat(suspendedUser.getSuspendedAt()).isNotNull();

        // when — reactivate Alice
        mockMvc.perform(post("/v1/merchants/{id}/users/{uid}/reactivate", merchantId, aliceId)
                        .header("Idempotency-Key", UUID.randomUUID().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status", is("ACTIVE")));

        // then — DB reflects active
        var reactivatedUser = userJpa.findById(aliceId).orElseThrow();
        assertThat(reactivatedUser.getStatus()).isEqualTo("ACTIVE");
        assertThat(reactivatedUser.getSuspendedAt()).isNull();

        // when — deactivate Alice
        mockMvc.perform(delete("/v1/merchants/{id}/users/{uid}", merchantId, aliceId)
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\": \"leaving company\"}"))
                .andExpect(status().isNoContent());

        // then — DB reflects deactivated
        var deactivatedUser = userJpa.findById(aliceId).orElseThrow();
        assertThat(deactivatedUser.getStatus()).isEqualTo("DEACTIVATED");
        assertThat(deactivatedUser.getDeactivatedAt()).isNotNull();

        // then — deactivated user not returned in active list
        mockMvc.perform(get("/v1/merchants/{id}/users?status=ACTIVE", merchantId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()", equalTo(1)));
    }

    // ── Flow 3: Change user role ─────────────────────────────────────────────

    @Test
    @DisplayName("should change user role and reflect in DB")
    void shouldChangeUserRole() throws Exception {
        // given — seed a second active user (Viewer)
        var viewerRoleId = roleJpa.findByMerchantIdAndRoleName(merchantId, "VIEWER").orElseThrow().getRoleId();
        var operatorRoleId = roleJpa.findByMerchantIdAndRoleName(merchantId, "PAYMENTS_OPERATOR").orElseThrow().getRoleId();
        var bobId = UUID.randomUUID();
        jdbc.update("""
            INSERT INTO merchant_users (user_id, merchant_id, email, email_hash, full_name, status,
                role_id, mfa_enabled, auth_provider, created_at, updated_at, activated_at, version)
            VALUES (?, ?, ?, ?, 'Bob', 'ACTIVE', ?, false, 'LOCAL', NOW(), NOW(), NOW(), 0)
            """, bobId, merchantId, "bob@acme.com".getBytes(), "bob-hash-bt", viewerRoleId);

        // when — change Bob's role to PAYMENTS_OPERATOR
        mockMvc.perform(patch("/v1/merchants/{id}/users/{uid}/role", merchantId, bobId)
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"roleId\": \"%s\"}".formatted(operatorRoleId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.oldRole", is("VIEWER")))
                .andExpect(jsonPath("$.data.newRole", is("PAYMENTS_OPERATOR")));

        // then — DB reflects new role
        var updatedUser = userJpa.findById(bobId).orElseThrow();
        assertThat(updatedUser.getRole().getRoleId()).isEqualTo(operatorRoleId);
    }
}
