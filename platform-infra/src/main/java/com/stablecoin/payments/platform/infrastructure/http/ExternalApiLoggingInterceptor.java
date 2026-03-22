package com.stablecoin.payments.platform.infrastructure.http;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.lang.Nullable;
import org.springframework.web.client.RestClient;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Set;

/**
 * Logs external API request/response details for debugging and sandbox testing.
 * <p>
 * Activated via {@code app.external-api.logging.enabled=true} (disabled by default).
 * Redacts sensitive headers (Authorization, X-API-KEY) to prevent credential leaks.
 * Response body is buffered so downstream consumers can still read it.
 */
public class ExternalApiLoggingInterceptor implements ClientHttpRequestInterceptor {

    private static final Logger log = LoggerFactory.getLogger(ExternalApiLoggingInterceptor.class);

    private static final Set<String> SENSITIVE_HEADERS = Set.of(
            "authorization", "x-api-key", "api-key", "x-api-secret"
    );
    private static final int REDACT_VISIBLE_CHARS = 8;

    private final int maxBodyLength;

    public ExternalApiLoggingInterceptor(int maxBodyLength) {
        this.maxBodyLength = maxBodyLength;
    }

    /**
     * Applies this interceptor to a {@link RestClient.Builder} if the interceptor is non-null.
     * Call this in adapter constructors: {@code ExternalApiLoggingInterceptor.applyTo(builder, interceptor)}
     */
    public static RestClient.Builder applyTo(RestClient.Builder builder,
                                              @Nullable ExternalApiLoggingInterceptor interceptor) {
        if (interceptor != null) {
            builder.requestInterceptors(list -> list.add(interceptor));
        }
        return builder;
    }

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body,
                                         ClientHttpRequestExecution execution) throws IOException {
        var startMs = System.currentTimeMillis();
        logRequest(request, body);

        var response = execution.execute(request, body);

        var latencyMs = System.currentTimeMillis() - startMs;
        var buffered = new BufferedResponse(response);
        logResponse(request, buffered, latencyMs);
        return buffered;
    }

    private void logRequest(HttpRequest request, byte[] body) {
        if (!log.isDebugEnabled()) {
            return;
        }
        log.debug("--> {} {}", request.getMethod(), request.getURI());
        logHeaders("-->", request.getHeaders());
        if (body.length > 0) {
            log.debug("--> Body: {}", truncate(new String(body, StandardCharsets.UTF_8)));
        }
    }

    private void logResponse(HttpRequest request, BufferedResponse response, long latencyMs) {
        if (!log.isDebugEnabled()) {
            return;
        }
        log.debug("<-- {} {} ({}ms)", response.statusCode().value(), request.getURI(), latencyMs);
        var responseBody = new String(response.bodyBytes, StandardCharsets.UTF_8);
        if (!responseBody.isEmpty()) {
            log.debug("<-- Body: {}", truncate(responseBody));
        }
    }

    private void logHeaders(String prefix, HttpHeaders headers) {
        headers.forEach((name, values) -> {
            var displayValue = SENSITIVE_HEADERS.contains(name.toLowerCase())
                    ? redact(values.getFirst())
                    : String.join(", ", values);
            log.debug("{} {}: {}", prefix, name, displayValue);
        });
    }

    private String truncate(String value) {
        if (value.length() <= maxBodyLength) {
            return value;
        }
        return value.substring(0, maxBodyLength) + "...[truncated, total=" + value.length() + "]";
    }

    static String redact(String value) {
        if (value == null || value.length() <= REDACT_VISIBLE_CHARS) {
            return "***";
        }
        return value.substring(0, REDACT_VISIBLE_CHARS) + "***";
    }

    /**
     * Wraps a response to buffer its body so it can be read multiple times
     * (once for logging, once for the actual consumer).
     */
    private static final class BufferedResponse implements ClientHttpResponse {

        private final ClientHttpResponse delegate;
        final byte[] bodyBytes;

        BufferedResponse(ClientHttpResponse delegate) throws IOException {
            this.delegate = delegate;
            this.bodyBytes = delegate.getBody().readAllBytes();
        }

        @Override
        public HttpStatusCode getStatusCode() {
            try {
                return delegate.getStatusCode();
            } catch (IOException e) {
                throw new IllegalStateException("Failed to read status code", e);
            }
        }

        HttpStatusCode statusCode() {
            return getStatusCode();
        }

        @Override
        public String getStatusText() throws IOException {
            return delegate.getStatusText();
        }

        @Override
        public void close() {
            delegate.close();
        }

        @Override
        public InputStream getBody() {
            return new ByteArrayInputStream(bodyBytes);
        }

        @Override
        public HttpHeaders getHeaders() {
            return delegate.getHeaders();
        }
    }
}
