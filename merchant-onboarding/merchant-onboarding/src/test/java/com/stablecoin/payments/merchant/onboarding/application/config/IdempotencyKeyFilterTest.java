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
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

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
        filter = spy(new IdempotencyKeyFilter(jdbcTemplate, 24));
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
        @DisplayName("should replay stored response when reservation fails and hash matches")
        void shouldReplayStoredResponse_whenSameKeyAndSameHash() throws ServletException, IOException {
            request.addHeader("Idempotency-Key", "key-123");
            request.setContent(REQUEST_BODY.getBytes(StandardCharsets.UTF_8));

            doReturn(false).when(filter).reserveIdempotencyKey("key-123", "POST", "/api/v1/merchants", REQUEST_HASH);
            doReturn(new IdempotencyKeyFilter.IdempotencyRecord("key-123", REQUEST_HASH, "{\"id\":\"abc\"}", 201))
                    .when(filter).lookupIdempotencyKey("key-123", "POST", "/api/v1/merchants");

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
        @DisplayName("should return 422 when reservation fails and hash differs")
        void shouldReturn422WhenSameKeyDifferentHash() throws ServletException, IOException {
            request.addHeader("Idempotency-Key", "key-456");
            request.setContent(REQUEST_BODY.getBytes(StandardCharsets.UTF_8));

            doReturn(false).when(filter).reserveIdempotencyKey("key-456", "POST", "/api/v1/merchants", REQUEST_HASH);
            doReturn(new IdempotencyKeyFilter.IdempotencyRecord("key-456", "different-hash", "{}", 201))
                    .when(filter).lookupIdempotencyKey("key-456", "POST", "/api/v1/merchants");

            filter.doFilterInternal(request, response, filterChain);

            assertThat(response.getStatus()).isEqualTo(422);
            assertThat(response.getContentAsString()).contains("MO-0002");
            then(filterChain).shouldHaveNoInteractions();
        }
    }

    @Nested
    @DisplayName("Reservation succeeds — proceed and finalize")
    class ProceedAndPersist {

        @Test
        @DisplayName("should proceed with chain when reservation succeeds")
        void shouldProceedAndFinalize_whenReservationSucceeds() throws ServletException, IOException {
            request.addHeader("Idempotency-Key", "key-789");
            request.setContent(REQUEST_BODY.getBytes(StandardCharsets.UTF_8));

            doReturn(true).when(filter).reserveIdempotencyKey("key-789", "POST", "/api/v1/merchants", REQUEST_HASH);

            filter.doFilterInternal(request, response, filterChain);

            assertThat(response.getStatus()).isEqualTo(200);
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

    @Nested
    @DisplayName("Conflict — in-flight request")
    class InFlightConflict {

        @Test
        @DisplayName("should return 409 when reservation fails and stored record has status_code=0")
        void shouldReturn409WhenRequestInFlight() throws ServletException, IOException {
            request.addHeader("Idempotency-Key", "key-inflight");
            request.setContent(REQUEST_BODY.getBytes(StandardCharsets.UTF_8));

            doReturn(false).when(filter).reserveIdempotencyKey("key-inflight", "POST", "/api/v1/merchants", REQUEST_HASH);
            doReturn(new IdempotencyKeyFilter.IdempotencyRecord("key-inflight", REQUEST_HASH, "", 0))
                    .when(filter).lookupIdempotencyKey("key-inflight", "POST", "/api/v1/merchants");

            filter.doFilterInternal(request, response, filterChain);

            assertThat(response.getStatus()).isEqualTo(409);
            assertThat(response.getContentAsString()).contains("MO-0003");
            then(filterChain).shouldHaveNoInteractions();
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
