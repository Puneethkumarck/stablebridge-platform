package com.stablecoin.payments.phase3.support;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.awaitility.Awaitility.await;

/**
 * Checks that all 7 Phase 3 services are healthy before tests run.
 * Polls each service's actuator/health endpoint.
 */
public final class ServiceHealthChecker {

    private static final Logger log = LoggerFactory.getLogger(ServiceHealthChecker.class);

    private static final Map<String, String> SERVICE_HEALTH_URLS = new LinkedHashMap<>();

    static {
        SERVICE_HEALTH_URLS.put("S1 Payment Orchestrator", "http://localhost:8082/orchestrator/actuator/health");
        SERVICE_HEALTH_URLS.put("S2 Compliance", "http://localhost:8083/compliance/actuator/health");
        SERVICE_HEALTH_URLS.put("S6 FX Engine", "http://localhost:8084/fx/actuator/health");
        SERVICE_HEALTH_URLS.put("S3 Fiat On-Ramp", "http://localhost:8085/on-ramp/actuator/health");
        SERVICE_HEALTH_URLS.put("S4 Blockchain Custody", "http://localhost:8086/custody/actuator/health");
        SERVICE_HEALTH_URLS.put("S5 Fiat Off-Ramp", "http://localhost:8087/off-ramp/actuator/health");
        SERVICE_HEALTH_URLS.put("S7 Ledger Accounting", "http://localhost:8088/ledger/actuator/health");
    }

    private final HttpClient httpClient;

    public ServiceHealthChecker(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    /**
     * Waits until all 7 services report healthy.
     * Polls each service every 5 seconds up to the given timeout.
     *
     * @param timeout maximum time to wait for all services
     * @throws org.awaitility.core.ConditionTimeoutException if any service is not healthy within timeout
     */
    public void waitForAllServices(Duration timeout) {
        log.info("Waiting for all 7 Phase 3 services to become healthy (timeout: {})", timeout);

        for (Map.Entry<String, String> entry : SERVICE_HEALTH_URLS.entrySet()) {
            var serviceName = entry.getKey();
            var healthUrl = entry.getValue();

            log.info("Checking health of {} at {}", serviceName, healthUrl);
            await().atMost(timeout)
                    .pollInterval(Duration.ofSeconds(5))
                    .ignoreExceptions()
                    .until(() -> isHealthy(serviceName, healthUrl));
            log.info("{} is healthy", serviceName);
        }

        log.info("All 7 Phase 3 services are healthy");
    }

    private boolean isHealthy(String serviceName, String healthUrl) {
        try {
            var response = httpClient.send(
                    HttpRequest.newBuilder()
                            .uri(URI.create(healthUrl))
                            .timeout(Duration.ofSeconds(5))
                            .GET()
                            .build(),
                    HttpResponse.BodyHandlers.ofString());

            var healthy = response.statusCode() == 200;
            if (!healthy) {
                log.debug("{} returned status {}", serviceName, response.statusCode());
            }
            return healthy;
        } catch (Exception e) {
            log.debug("{} health check failed: {}", serviceName, e.getMessage());
            return false;
        }
    }
}
