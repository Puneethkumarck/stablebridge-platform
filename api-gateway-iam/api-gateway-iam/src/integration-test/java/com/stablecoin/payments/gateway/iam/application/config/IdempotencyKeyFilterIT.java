package com.stablecoin.payments.gateway.iam.application.config;

import com.stablecoin.payments.gateway.iam.AbstractIntegrationTest;
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

    private static final String MERCHANT_ENDPOINT = "/v1/merchants";

    @Test
    @DisplayName("should persist idempotency key after successful mutation")
    void shouldPersistIdempotencyKey_afterSuccessfulMutation() throws Exception {
        var idempotencyKey = UUID.randomUUID().toString();
        var externalId = UUID.randomUUID();

        mockMvc.perform(post(MERCHANT_ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Idempotency-Key", idempotencyKey)
                        .content("""
                                {
                                    "externalId": "%s",
                                    "name": "Idempotency Test Corp",
                                    "country": "US"
                                }
                                """.formatted(externalId)))
                .andExpect(status().isCreated());

        var count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM gatewayiam_idempotency_keys WHERE idempotency_key = ?",
                Integer.class, idempotencyKey);

        assertThat(count).isEqualTo(1);
    }

    @Test
    @DisplayName("should replay response on duplicate request with same key and body")
    void shouldReplayResponse_onDuplicateRequest() throws Exception {
        var idempotencyKey = UUID.randomUUID().toString();
        var externalId = UUID.randomUUID();
        var requestBody = """
                {
                    "externalId": "%s",
                    "name": "Replay Test Corp",
                    "country": "US"
                }
                """.formatted(externalId);

        // First request
        var firstResponse = mockMvc.perform(post(MERCHANT_ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Idempotency-Key", idempotencyKey)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andReturn();

        // Second request — same key, same body
        var secondResponse = mockMvc.perform(post(MERCHANT_ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Idempotency-Key", idempotencyKey)
                        .content(requestBody))
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
        mockMvc.perform(post(MERCHANT_ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Idempotency-Key", idempotencyKey)
                        .content("""
                                {
                                    "externalId": "%s",
                                    "name": "First Corp",
                                    "country": "US"
                                }
                                """.formatted(UUID.randomUUID())))
                .andExpect(status().isCreated());

        // Second request — same key, different body
        mockMvc.perform(post(MERCHANT_ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Idempotency-Key", idempotencyKey)
                        .content("""
                                {
                                    "externalId": "%s",
                                    "name": "Different Corp",
                                    "country": "GB"
                                }
                                """.formatted(UUID.randomUUID())))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @DisplayName("should delete expired keys when cleanup job runs")
    void shouldDeleteExpiredKeys_whenCleanupJobRuns() throws Exception {
        // Insert an expired idempotency key directly
        jdbcTemplate.update(
                "INSERT INTO gatewayiam_idempotency_keys"
                        + " (idempotency_key, request_method, request_path, request_hash, response_body, status_code, expires_at)"
                        + " VALUES (?, ?, ?, ?, ?, ?, ?)",
                "expired-key", "POST", "/v1/merchants", "somehash", "{}", 200,
                Timestamp.from(Instant.now().minus(1, ChronoUnit.HOURS)));

        var cleanupJob = new com.stablecoin.payments.gateway.iam.application.job.IdempotencyCleanupJob(jdbcTemplate);
        cleanupJob.cleanExpiredKeys();

        var count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM gatewayiam_idempotency_keys WHERE idempotency_key = ?",
                Integer.class, "expired-key");

        assertThat(count).isEqualTo(0);
    }
}
