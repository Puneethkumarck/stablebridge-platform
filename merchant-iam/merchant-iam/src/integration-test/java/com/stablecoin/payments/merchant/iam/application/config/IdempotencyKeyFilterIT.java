package com.stablecoin.payments.merchant.iam.application.config;

import com.stablecoin.payments.merchant.iam.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("IdempotencyKeyFilter IT")
class IdempotencyKeyFilterIT extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private UUID merchantId;

    @BeforeEach
    void seedBuiltInRoles() {
        merchantId = UUID.randomUUID();

        // Seed an ADMIN role so createRole controller can resolve caller
        var adminRoleId = UUID.randomUUID();
        jdbcTemplate.update("""
            INSERT INTO roles (role_id, merchant_id, role_name, description, is_builtin, is_active, created_at, updated_at)
            VALUES (?, ?, 'ADMIN', 'Administrator', true, true, NOW(), NOW())
            """, adminRoleId, merchantId);

        jdbcTemplate.update(
                "INSERT INTO role_permissions (role_permission_id, role_id, permission, created_at) VALUES (?, ?, '*:*', NOW())",
                UUID.randomUUID(), adminRoleId);

        jdbcTemplate.update("""
            INSERT INTO merchant_users (user_id, merchant_id, email, email_hash, full_name, status,
                role_id, mfa_enabled, auth_provider, created_at, updated_at, activated_at, version)
            VALUES (?, ?, ?, 'admin-hash-idem', 'Admin', 'ACTIVE',
                ?, false, 'LOCAL', NOW(), NOW(), NOW(), 0)
            """, UUID.fromString("00000000-0000-0000-0000-000000000001"),
                merchantId, "admin-idem@acme.com".getBytes(), adminRoleId);
    }

    @Test
    @DisplayName("should persist idempotency key after successful mutation")
    void shouldPersistIdempotencyKey_afterSuccessfulMutation() throws Exception {
        var idempotencyKey = UUID.randomUUID().toString();

        mockMvc.perform(withIdempotencyKey(idempotencyKey,
                        post("/v1/merchants/{merchantId}/roles", merchantId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(roleRequestBody("FINANCE_OPS"))))
                .andExpect(status().isCreated());

        var count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM merchantiam_idempotency_keys WHERE idempotency_key = ?",
                Integer.class, idempotencyKey);

        assertThat(count).isEqualTo(1);
    }

    @Test
    @DisplayName("should replay response on duplicate request with same key and body")
    void shouldReplayResponse_onDuplicateRequest() throws Exception {
        var idempotencyKey = UUID.randomUUID().toString();
        var requestBody = roleRequestBody("REPLAY_ROLE");

        // First request
        var firstResponse = mockMvc.perform(withIdempotencyKey(idempotencyKey,
                        post("/v1/merchants/{merchantId}/roles", merchantId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody)))
                .andExpect(status().isCreated())
                .andReturn();

        // Second request — same key, same body
        var secondResponse = mockMvc.perform(withIdempotencyKey(idempotencyKey,
                        post("/v1/merchants/{merchantId}/roles", merchantId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Idempotency-Replay", "true"))
                .andReturn();

        assertThat(secondResponse.getResponse().getContentAsString())
                .isEqualTo(firstResponse.getResponse().getContentAsString());
    }

    @Test
    @DisplayName("should return 422 when same key but different body")
    void shouldReturn422_whenSameKeyDifferentBody() throws Exception {
        var idempotencyKey = UUID.randomUUID().toString();

        // First request
        mockMvc.perform(withIdempotencyKey(idempotencyKey,
                        post("/v1/merchants/{merchantId}/roles", merchantId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(roleRequestBody("FIRST_ROLE"))))
                .andExpect(status().isCreated());

        // Second request — same key, different body
        mockMvc.perform(withIdempotencyKey(idempotencyKey,
                        post("/v1/merchants/{merchantId}/roles", merchantId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(roleRequestBody("DIFFERENT_ROLE"))))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @DisplayName("should delete expired keys when cleanup job runs")
    void shouldDeleteExpiredKeys_whenCleanupJobRuns() throws Exception {
        jdbcTemplate.update(
                "INSERT INTO merchantiam_idempotency_keys (idempotency_key, request_hash, response_body, status_code, expires_at) "
                        + "VALUES (?, ?, ?, ?, ?)",
                "expired-key", "somehash", "{}", 200,
                Timestamp.from(Instant.now().minus(1, ChronoUnit.HOURS)));

        var cleanupJob = new com.stablecoin.payments.merchant.iam.application.job.IdempotencyCleanupJob(jdbcTemplate);
        cleanupJob.cleanExpiredKeys();

        var count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM merchantiam_idempotency_keys WHERE idempotency_key = ?",
                Integer.class, "expired-key");

        assertThat(count).isEqualTo(0);
    }

    private static org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder withIdempotencyKey(
            String key, org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder builder) {
        return builder.header("Idempotency-Key", key);
    }

    private String roleRequestBody(String roleName) {
        return """
                {
                    "roleName": "%s",
                    "description": "Test role",
                    "permissions": ["payments:read"]
                }
                """.formatted(roleName);
    }
}
