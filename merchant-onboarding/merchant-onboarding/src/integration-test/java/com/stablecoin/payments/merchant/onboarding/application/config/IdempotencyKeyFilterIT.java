package com.stablecoin.payments.merchant.onboarding.application.config;

import com.stablecoin.payments.merchant.onboarding.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
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

    private static final String MERCHANT_ENDPOINT = "/api/v1/merchants";

    @BeforeEach
    void cleanIdempotencyKeys() {
        jdbcTemplate.execute("DELETE FROM onboarding_idempotency_keys");
    }

    @Test
    @DisplayName("should persist idempotency key after successful mutation")
    @WithMockUser(authorities = "merchant:write")
    void shouldPersistIdempotencyKey_afterSuccessfulMutation() throws Exception {
        var idempotencyKey = UUID.randomUUID().toString();

        mockMvc.perform(post(MERCHANT_ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Idempotency-Key", idempotencyKey)
                        .content(merchantRequestBody()))
                .andExpect(status().isCreated());

        var count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM onboarding_idempotency_keys WHERE idempotency_key = ?",
                Integer.class, idempotencyKey);

        assertThat(count).isEqualTo(1);
    }

    @Test
    @DisplayName("should replay response on duplicate request with same key and body")
    @WithMockUser(authorities = "merchant:write")
    void shouldReplayResponse_onDuplicateRequest() throws Exception {
        var idempotencyKey = UUID.randomUUID().toString();
        var requestBody = merchantRequestBody();

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
    @WithMockUser(authorities = "merchant:write")
    void shouldReturn422_whenSameKeyDifferentBody() throws Exception {
        var idempotencyKey = UUID.randomUUID().toString();

        // First request
        mockMvc.perform(post(MERCHANT_ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Idempotency-Key", idempotencyKey)
                        .content(merchantRequestBody()))
                .andExpect(status().isCreated());

        // Second request — same key, different body
        mockMvc.perform(post(MERCHANT_ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Idempotency-Key", idempotencyKey)
                        .content(differentMerchantRequestBody()))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @DisplayName("should delete expired keys when cleanup job runs")
    void shouldDeleteExpiredKeys_whenCleanupJobRuns() throws Exception {
        jdbcTemplate.update(
                "INSERT INTO onboarding_idempotency_keys"
                        + " (idempotency_key, request_method, request_path, request_hash, response_body, status_code, expires_at)"
                        + " VALUES (?, ?, ?, ?, ?, ?, ?)",
                "expired-key", "POST", "/api/v1/merchants", "somehash", "{}", 200,
                Timestamp.from(Instant.now().minus(1, ChronoUnit.HOURS)));

        var cleanupJob = new com.stablecoin.payments.merchant.onboarding.application.job.IdempotencyCleanupJob(jdbcTemplate);
        cleanupJob.cleanExpiredKeys();

        var count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM onboarding_idempotency_keys WHERE idempotency_key = ?",
                Integer.class, "expired-key");

        assertThat(count).isEqualTo(0);
    }

    private String merchantRequestBody() {
        return """
                {
                    "legalName": "Test Corp %s",
                    "tradingName": "TestCo",
                    "registrationNumber": "REG-%s",
                    "registrationCountry": "GB",
                    "entityType": "PRIVATE_LIMITED",
                    "websiteUrl": "https://testcorp.com",
                    "primaryCurrency": "USD",
                    "primaryContactEmail": "john@testcorp.com",
                    "primaryContactName": "John Doe",
                    "registeredAddress": {
                        "streetLine1": "1 Test Street",
                        "city": "London",
                        "postcode": "EC1A 1BB",
                        "country": "GB"
                    },
                    "beneficialOwners": [{
                        "fullName": "John Doe",
                        "dateOfBirth": "1985-01-15",
                        "nationality": "GB",
                        "ownershipPct": 100.00,
                        "isPoliticallyExposed": false
                    }],
                    "requestedCorridors": ["GB->US"]
                }
                """.formatted(UUID.randomUUID(), UUID.randomUUID());
    }

    private String differentMerchantRequestBody() {
        return """
                {
                    "legalName": "Different Corp %s",
                    "tradingName": "DiffCo",
                    "registrationNumber": "REG-%s",
                    "registrationCountry": "US",
                    "entityType": "PRIVATE_LIMITED",
                    "websiteUrl": "https://diffcorp.com",
                    "primaryCurrency": "EUR",
                    "primaryContactEmail": "jane@diffcorp.com",
                    "primaryContactName": "Jane Doe",
                    "registeredAddress": {
                        "streetLine1": "2 Other Street",
                        "city": "New York",
                        "postcode": "10001",
                        "country": "US"
                    },
                    "beneficialOwners": [{
                        "fullName": "Jane Doe",
                        "dateOfBirth": "1990-06-20",
                        "nationality": "US",
                        "ownershipPct": 100.00,
                        "isPoliticallyExposed": false
                    }],
                    "requestedCorridors": ["US->GB"]
                }
                """.formatted(UUID.randomUUID(), UUID.randomUUID());
    }
}
