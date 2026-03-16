package com.stablecoin.payments.gateway.iam.application.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.Set;

/**
 * Enforces presence of {@code Idempotency-Key} header on state-mutating endpoints
 * (POST, PATCH, DELETE) — excluding auth, JWKS, and actuator endpoints.
 * Persists idempotency key + request hash + response for duplicate detection and replay.
 */
@Slf4j
@Component
@Order(2)
public class IdempotencyKeyFilter extends OncePerRequestFilter {

    public static final String IDEMPOTENCY_KEY_HEADER = "Idempotency-Key";
    public static final String IDEMPOTENCY_REPLAY_HEADER = "Idempotency-Replay";

    private static final String TABLE_NAME = "gatewayiam_idempotency_keys";
    private static final Set<String> MUTATING_METHODS = Set.of("POST", "PUT", "PATCH", "DELETE");
    private static final Set<String> EXEMPT_PREFIXES = Set.of(
            "/v1/auth/",
            "/.well-known/",
            "/actuator/"
    );

    private final JdbcTemplate jdbcTemplate;
    private final long ttlHours;

    public IdempotencyKeyFilter(JdbcTemplate jdbcTemplate,
                                @Value("${app.idempotency.ttl-hours:24}") long ttlHours) {
        this.jdbcTemplate = jdbcTemplate;
        this.ttlHours = ttlHours;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        if (!requiresIdempotencyKey(request)) {
            chain.doFilter(request, response);
            return;
        }

        var key = request.getHeader(IDEMPOTENCY_KEY_HEADER);
        if (key == null || key.isBlank()) {
            log.info("Missing Idempotency-Key header for {} {}", request.getMethod(), request.getRequestURI());
            response.setStatus(HttpStatus.BAD_REQUEST.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write(
                    "{\"code\":\"GW-0001\",\"status\":\"Bad Request\","
                            + "\"message\":\"Idempotency-Key header is required for mutating requests\","
                            + "\"errors\":{}}");
            return;
        }

        var bodyBytes = request.getInputStream().readAllBytes();
        var requestHash = computeSha256(bodyBytes);

        var existing = lookupIdempotencyKey(key);
        if (existing != null) {
            if (existing.requestHash().equals(requestHash)) {
                replayResponse(response, existing);
                return;
            }
            writeHashMismatchError(response);
            return;
        }

        var replayableRequest = new CachedBodyRequestWrapper(request, bodyBytes);
        var wrappedResponse = new ContentCachingResponseWrapper(response);
        chain.doFilter(replayableRequest, wrappedResponse);

        persistIdempotencyKey(key, requestHash, wrappedResponse);
        wrappedResponse.copyBodyToResponse();
    }

    private boolean requiresIdempotencyKey(HttpServletRequest request) {
        if (!MUTATING_METHODS.contains(request.getMethod())) {
            return false;
        }
        var uri = request.getRequestURI();
        var contextPath = request.getContextPath();
        var path = contextPath.isEmpty() ? uri : uri.substring(contextPath.length());
        return EXEMPT_PREFIXES.stream().noneMatch(path::startsWith);
    }

    private IdempotencyRecord lookupIdempotencyKey(String key) {
        var results = jdbcTemplate.query(
                "SELECT idempotency_key, request_hash, response_body, status_code FROM " + TABLE_NAME
                        + " WHERE idempotency_key = ?",
                (rs, rowNum) -> new IdempotencyRecord(
                        rs.getString("idempotency_key"),
                        rs.getString("request_hash"),
                        rs.getString("response_body"),
                        rs.getInt("status_code")),
                key);
        return results.isEmpty() ? null : results.getFirst();
    }

    private void persistIdempotencyKey(String key, String requestHash,
                                       ContentCachingResponseWrapper wrappedResponse) {
        var responseBody = new String(wrappedResponse.getContentAsByteArray(), StandardCharsets.UTF_8);
        var statusCode = wrappedResponse.getStatus();
        var expiresAt = Timestamp.from(Instant.now().plus(ttlHours, ChronoUnit.HOURS));

        try {
            jdbcTemplate.update(
                    "INSERT INTO " + TABLE_NAME
                            + " (idempotency_key, request_hash, response_body, status_code, expires_at)"
                            + " VALUES (?, ?, ?, ?, ?)",
                    key, requestHash, responseBody, statusCode, expiresAt);
        } catch (DataIntegrityViolationException e) {
            log.debug("Concurrent idempotency key insertion for key={}", key);
            var existing = lookupIdempotencyKey(key);
            if (existing != null && !existing.requestHash().equals(requestHash)) {
                log.warn("Concurrent idempotency key conflict: key={}, stored hash differs", key);
            }
        }
    }

    private void replayResponse(HttpServletResponse response, IdempotencyRecord record)
            throws IOException {
        log.info("Replaying idempotent response for key={}", record.idempotencyKey());
        response.setStatus(record.statusCode());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setHeader(IDEMPOTENCY_REPLAY_HEADER, "true");
        response.getWriter().write(record.responseBody());
    }

    private void writeHashMismatchError(HttpServletResponse response) throws IOException {
        response.setStatus(HttpStatus.UNPROCESSABLE_ENTITY.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(
                "{\"code\":\"GW-0002\",\"status\":\"Unprocessable Entity\","
                        + "\"message\":\"Idempotency-Key has already been used with a different request payload\","
                        + "\"errors\":{}}");
    }

    private String computeSha256(byte[] body) {
        try {
            var digest = MessageDigest.getInstance("SHA-256");
            var hash = digest.digest(body);
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    record IdempotencyRecord(String idempotencyKey, String requestHash, String responseBody, int statusCode) {}

    /**
     * Request wrapper that replays a cached body so downstream filters and
     * controllers can read the request body after it has already been consumed.
     */
    private static class CachedBodyRequestWrapper extends HttpServletRequestWrapper {

        private final byte[] body;

        CachedBodyRequestWrapper(HttpServletRequest request, byte[] body) {
            super(request);
            this.body = body;
        }

        @Override
        public ServletInputStream getInputStream() {
            var byteStream = new ByteArrayInputStream(body);
            return new ServletInputStream() {
                @Override
                public boolean isFinished() {
                    return byteStream.available() == 0;
                }

                @Override
                public boolean isReady() {
                    return true;
                }

                @Override
                public void setReadListener(ReadListener readListener) {
                    throw new UnsupportedOperationException();
                }

                @Override
                public int read() {
                    return byteStream.read();
                }

                @Override
                public int read(byte[] b, int off, int len) {
                    return byteStream.read(b, off, len);
                }
            };
        }

        @Override
        public BufferedReader getReader() {
            return new BufferedReader(new InputStreamReader(getInputStream(), StandardCharsets.UTF_8));
        }

        @Override
        public int getContentLength() {
            return body.length;
        }

        @Override
        public long getContentLengthLong() {
            return body.length;
        }
    }
}
