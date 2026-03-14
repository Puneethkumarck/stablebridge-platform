package com.stablecoin.payments.phase3.support;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/**
 * Client for managing WireMock stubs dynamically via the admin API.
 */
public final class WireMockAdminClient {

    private static final Logger log = LoggerFactory.getLogger(WireMockAdminClient.class);

    private static final String DEFAULT_BASE_URL = "http://localhost:4444";

    private final HttpClient httpClient;
    private final String baseUrl;
    private final JsonMapper jsonMapper;

    public WireMockAdminClient(HttpClient httpClient, String baseUrl) {
        this.httpClient = httpClient;
        this.baseUrl = baseUrl;
        this.jsonMapper = JsonMapper.builder().findAndAddModules().build();
    }

    public WireMockAdminClient(HttpClient httpClient) {
        this(httpClient, DEFAULT_BASE_URL);
    }

    /**
     * Resets all WireMock mappings back to the defaults (from JSON files).
     */
    public void resetMappings() throws Exception {
        var response = httpClient.send(
                HttpRequest.newBuilder()
                        .uri(URI.create(baseUrl + "/__admin/mappings/reset"))
                        .POST(HttpRequest.BodyPublishers.noBody())
                        .build(),
                HttpResponse.BodyHandlers.ofString());

        log.info("WireMock mappings reset: status={}", response.statusCode());
    }

    /**
     * Adds a new WireMock mapping (stub).
     *
     * @param jsonBody the mapping JSON body
     */
    public void addMapping(String jsonBody) throws Exception {
        var response = httpClient.send(
                HttpRequest.newBuilder()
                        .uri(URI.create(baseUrl + "/__admin/mappings"))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                        .build(),
                HttpResponse.BodyHandlers.ofString());

        log.info("WireMock mapping added: status={}", response.statusCode());
    }

    /**
     * Gets all recorded requests matching the given URL path.
     *
     * @param path the URL path to filter by (e.g., "/v2/cases/screeningRequest")
     * @return the JSON response containing matching requests
     */
    public JsonNode getRequestsForPath(String path) throws Exception {
        var filterBody = """
                {
                    "urlPattern": "%s"
                }
                """.formatted(path);

        var response = httpClient.send(
                HttpRequest.newBuilder()
                        .uri(URI.create(baseUrl + "/__admin/requests/find"))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(filterBody))
                        .build(),
                HttpResponse.BodyHandlers.ofString());

        return jsonMapper.readTree(response.body());
    }
}
