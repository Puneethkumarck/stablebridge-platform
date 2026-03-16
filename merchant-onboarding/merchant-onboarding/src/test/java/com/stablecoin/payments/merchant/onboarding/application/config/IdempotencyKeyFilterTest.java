package com.stablecoin.payments.merchant.onboarding.application.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.HexFormat;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("IdempotencyKeyFilter")
class IdempotencyKeyFilterTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Mock
    private FilterChain filterChain;

    private IdempotencyKeyFilter filter;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    private static final String REQUEST_BODY = "{\"name\":\"test\"}";
    private static final String REQUEST_HASH = computeHash(REQUEST_BODY.getBytes(StandardCharsets.UTF_8));

    @BeforeEach
    void setUp() {
        filter = new IdempotencyKeyFilter(jdbcTemplate, 24);
        request = new MockHttpServletRequest("POST", "/api/v1/merchants");
        response = new MockHttpServletResponse();
    }

    @Nested
    @DisplayName("Missing Idempotency-Key")
    class MissingKey {

        @Test
        @DisplayName("should return 400 when Idempotency-Key header is missing")
        void shouldReturn400WhenIdempotencyKeyMissing() throws ServletException, IOException {
            filter.doFilterInternal(request, response, filterChain);

            assertThat(response.getStatus()).isEqualTo(400);
            assertThat(response.getContentAsString()).contains("MO-0001");
        }
    }

    @Nested
    @DisplayName("Replay stored response")
    class ReplayResponse {

        @Test
        @DisplayName("should replay stored response when same key and same hash")
        @SuppressWarnings("unchecked")
        void shouldReplayStoredResponse_whenSameKeyAndSameHash() throws ServletException, IOException {
            request.addHeader("Idempotency-Key", "key-123");
            request.setContent(REQUEST_BODY.getBytes(StandardCharsets.UTF_8));

            var storedRecord = new IdempotencyKeyFilter.IdempotencyRecord(
                    "key-123", REQUEST_HASH, "{\"id\":\"abc\"}", 201
            );

            given(jdbcTemplate.query(
                    startsWith("SELECT"),
                    org.mockito.ArgumentMatchers.<RowMapper<IdempotencyKeyFilter.IdempotencyRecord>>any(),
                    org.mockito.ArgumentMatchers.eq("key-123")
            )).willReturn(List.of(storedRecord));

            filter.doFilterInternal(request, response, filterChain);

            assertThat(response.getStatus()).isEqualTo(201);
            assertThat(response.getContentAsString()).isEqualTo("{\"id\":\"abc\"}");
            assertThat(response.getHeader("Idempotency-Replay")).isEqualTo("true");
            then(filterChain).shouldHaveNoInteractions();
        }
    }

    @Nested
    @DisplayName("Hash mismatch")
    class HashMismatch {

        @Test
        @DisplayName("should return 422 when same key but different hash")
        @SuppressWarnings("unchecked")
        void shouldReturn422WhenSameKeyDifferentHash() throws ServletException, IOException {
            request.addHeader("Idempotency-Key", "key-456");
            request.setContent(REQUEST_BODY.getBytes(StandardCharsets.UTF_8));

            var storedRecord = new IdempotencyKeyFilter.IdempotencyRecord(
                    "key-456", "different-hash-value", "{\"id\":\"abc\"}", 201
            );

            given(jdbcTemplate.query(
                    startsWith("SELECT"),
                    org.mockito.ArgumentMatchers.<RowMapper<IdempotencyKeyFilter.IdempotencyRecord>>any(),
                    org.mockito.ArgumentMatchers.eq("key-456")
            )).willReturn(List.of(storedRecord));

            filter.doFilterInternal(request, response, filterChain);

            assertThat(response.getStatus()).isEqualTo(422);
            assertThat(response.getContentAsString()).contains("MO-0002");
            then(filterChain).shouldHaveNoInteractions();
        }
    }

    @Nested
    @DisplayName("Key not found — proceed and persist")
    class ProceedAndPersist {

        @Test
        @DisplayName("should proceed and persist when key is not found")
        @SuppressWarnings("unchecked")
        void shouldProceedAndPersist_whenKeyNotFound() throws ServletException, IOException {
            request.addHeader("Idempotency-Key", "key-789");
            request.setContent(REQUEST_BODY.getBytes(StandardCharsets.UTF_8));

            given(jdbcTemplate.query(
                    startsWith("SELECT"),
                    org.mockito.ArgumentMatchers.<RowMapper<IdempotencyKeyFilter.IdempotencyRecord>>any(),
                    org.mockito.ArgumentMatchers.eq("key-789")
            )).willReturn(Collections.emptyList());

            filter.doFilterInternal(request, response, filterChain);

            then(filterChain).should().doFilter(
                    org.mockito.ArgumentMatchers.any(),
                    org.mockito.ArgumentMatchers.any()
            );
        }
    }

    @Nested
    @DisplayName("GET requests")
    class GetRequests {

        @Test
        @DisplayName("should skip idempotency check for GET requests")
        void shouldSkipIdempotencyCheckForGetRequests() throws ServletException, IOException {
            request = new MockHttpServletRequest("GET", "/api/v1/merchants");

            filter.doFilterInternal(request, response, filterChain);

            assertThat(response.getStatus()).isEqualTo(200);
            then(filterChain).should().doFilter(request, response);
            then(jdbcTemplate).shouldHaveNoInteractions();
        }
    }

    private static String computeHash(byte[] body) {
        try {
            var digest = MessageDigest.getInstance("SHA-256");
            var hash = digest.digest(body);
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
