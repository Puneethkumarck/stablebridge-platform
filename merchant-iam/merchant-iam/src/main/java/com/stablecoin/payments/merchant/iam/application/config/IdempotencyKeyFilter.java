package com.stablecoin.payments.merchant.iam.application.config;

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
 * (POST, PATCH, DELETE) -- excluding auth and invitation endpoints which use
 * tokens as implicit idempotency controls.
 * Uses INSERT-first reservation pattern to prevent TOCTOU races:
 * 1. Try INSERT with status_code=0 (reservation)
 * 2. If INSERT succeeds, proceed with request and UPDATE with real response
 * 3. If INSERT fails (duplicate), re-read stored record for replay or conflict
 */
@Slf4j
@Component
@Order(2)
public class IdempotencyKeyFilter extends OncePerRequestFilter {

    public static final String IDEMPOTENCY_KEY_HEADER = "Idempotency-Key";
    public static final String IDEMPOTENCY_REPLAY_HEADER = "Idempotency-Replay";

    private static final String TABLE_NAME = "merchantiam_idempotency_keys";
    private static final Set<String> MUTATING_METHODS = Set.of("POST", "PUT", "PATCH", "DELETE");
    private static final Set<String> EXEMPT_PREFIXES = Set.of(
            "/v1/auth/",
            "/v1/invitations/",
            "/v1/.well-known/",
            "/actuator/"
    );

    /** Path segments that mark a sub-resource as auth-related (login, refresh, logout, mfa). */
    private static final Set<String> AUTH_PATH_SEGMENTS = Set.of(
            "/auth/login", "/auth/refresh", "/auth/logout", "/auth/mfa"
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
                    "{\"code\":\"IAM-0001\",\"status\":\"Bad Request\","
                            + "\"message\":\"Idempotency-Key header is required for mutating requests\","
                            + "\"errors\":{}}");
            return;
        }

        var method = request.getMethod();
        var path = request.getRequestURI();
        var bodyBytes = request.getInputStream().readAllBytes();
        var requestHash = computeSha256(bodyBytes);

        var reserved = reserveIdempotencyKey(key, method, path, requestHash);
        if (reserved) {
            var replayableRequest = new CachedBodyRequestWrapper(request, bodyBytes);
            var wrappedResponse = new ContentCachingResponseWrapper(response);
            try {
                chain.doFilter(replayableRequest, wrappedResponse);
                finalizeIdempotencyKey(key, method, path, wrappedResponse);
                wrappedResponse.copyBodyToResponse();
            } catch (Exception e) {
                deleteReservation(key, method, path);
                throw e;
            }
            return;
        }

        var existing = lookupIdempotencyKey(key, method, path);
        if (existing == null) {
            writeConflictError(response);
            return;
        }
        if (existing.requestHash().equals(requestHash)) {
            if (existing.statusCode() == 0) {
                writeConflictError(response);
                return;
            }
            replayResponse(response, existing);
            return;
        }
        writeHashMismatchError(response);
    }

    private boolean requiresIdempotencyKey(HttpServletRequest request) {
        if (!MUTATING_METHODS.contains(request.getMethod())) {
            return false;
        }
        var uri = request.getRequestURI();
        var contextPath = request.getContextPath();
        var path = contextPath.isEmpty() ? uri : uri.substring(contextPath.length());
        if (EXEMPT_PREFIXES.stream().anyMatch(path::startsWith)) {
            return false;
        }
        // Exempt merchant-scoped auth endpoints: /v1/merchants/{id}/auth/login etc.
        return AUTH_PATH_SEGMENTS.stream().noneMatch(path::contains);
    }

    boolean reserveIdempotencyKey(String key, String method, String path, String requestHash) {
        try {
            jdbcTemplate.update(
                    "INSERT INTO " + TABLE_NAME
                            + " (idempotency_key, request_method, request_path, request_hash,"
                            + " response_body, status_code, expires_at)"
                            + " VALUES (?, ?, ?, ?, '', 0, ?)",
                    key, method, path, requestHash,
                    Timestamp.from(Instant.now().plus(ttlHours, ChronoUnit.HOURS)));
            return true;
        } catch (DataIntegrityViolationException e) {
            int deleted = jdbcTemplate.update(
                    "DELETE FROM " + TABLE_NAME
                            + " WHERE idempotency_key = ? AND request_method = ? AND request_path = ?"
                            + " AND expires_at <= NOW()",
                    key, method, path);
            if (deleted > 0) {
                try {
                    jdbcTemplate.update(
                            "INSERT INTO " + TABLE_NAME
                                    + " (idempotency_key, request_method, request_path, request_hash,"
                                    + " response_body, status_code, expires_at)"
                                    + " VALUES (?, ?, ?, ?, '', 0, ?)",
                            key, method, path, requestHash,
                            Timestamp.from(Instant.now().plus(ttlHours, ChronoUnit.HOURS)));
                    return true;
                } catch (DataIntegrityViolationException retryEx) {
                    return false;
                }
            }
            return false;
        }
    }

    IdempotencyRecord lookupIdempotencyKey(String key, String method, String path) {
        var results = jdbcTemplate.query(
                "SELECT idempotency_key, request_hash, response_body, status_code FROM " + TABLE_NAME
                        + " WHERE idempotency_key = ? AND request_method = ? AND request_path = ?"
                        + " AND expires_at > NOW()",
                (rs, rowNum) -> new IdempotencyRecord(
                        rs.getString("idempotency_key"),
                        rs.getString("request_hash"),
                        rs.getString("response_body"),
                        rs.getInt("status_code")),
                key, method, path);
        return results.isEmpty() ? null : results.getFirst();
    }

    void finalizeIdempotencyKey(String key, String method, String path,
                                ContentCachingResponseWrapper wrappedResponse) {
        var statusCode = wrappedResponse.getStatus();
        if (statusCode < 200 || statusCode >= 300) {
            deleteReservation(key, method, path);
            return;
        }
        var responseBody = new String(wrappedResponse.getContentAsByteArray(), StandardCharsets.UTF_8);
        var expiresAt = Timestamp.from(Instant.now().plus(ttlHours, ChronoUnit.HOURS));

        jdbcTemplate.update(
                "UPDATE " + TABLE_NAME
                        + " SET response_body = ?, status_code = ?, expires_at = ?"
                        + " WHERE idempotency_key = ? AND request_method = ? AND request_path = ?",
                responseBody, statusCode, expiresAt, key, method, path);
    }

    private void deleteReservation(String key, String method, String path) {
        jdbcTemplate.update(
                "DELETE FROM " + TABLE_NAME
                        + " WHERE idempotency_key = ? AND request_method = ? AND request_path = ?",
                key, method, path);
    }

    private void replayResponse(HttpServletResponse response, IdempotencyRecord record)
            throws IOException {
        log.info("Replaying idempotent response");
        response.setStatus(record.statusCode());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setHeader(IDEMPOTENCY_REPLAY_HEADER, "true");
        response.getWriter().write(record.responseBody());
    }

    private void writeHashMismatchError(HttpServletResponse response) throws IOException {
        response.setStatus(HttpStatus.UNPROCESSABLE_ENTITY.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(
                "{\"code\":\"IAM-0002\",\"status\":\"Unprocessable Entity\","
                        + "\"message\":\"Idempotency-Key has already been used with a different request payload\","
                        + "\"errors\":{}}");
    }

    private void writeConflictError(HttpServletResponse response) throws IOException {
        response.setStatus(HttpStatus.CONFLICT.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(
                "{\"code\":\"IAM-0003\",\"status\":\"Conflict\","
                        + "\"message\":\"A request with this Idempotency-Key is already in progress\","
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
