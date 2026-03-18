package com.stablecoin.payments.compliance.infrastructure.provider.ofacsdn;

import com.stablecoin.payments.compliance.domain.model.SanctionsResult;
import com.stablecoin.payments.compliance.domain.port.SanctionsProvider;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.net.http.HttpClient.Version;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Slf4j
@Component
@ConditionalOnProperty(name = "app.sanctions.provider", havingValue = "ofac-sdn")
@EnableConfigurationProperties(OfacSdnProperties.class)
public class OfacSdnSanctionsAdapter implements SanctionsProvider {

    private static final String PROVIDER_NAME = "ofac-sdn";
    private static final List<String> LISTS_CHECKED = List.of("OFAC_SDN");

    private final OfacSdnProperties properties;
    private final RestClient restClient;
    private final ReentrantReadWriteLock cacheLock = new ReentrantReadWriteLock();

    private volatile List<SdnEntry> cachedEntries;
    private volatile Instant lastRefresh;

    public OfacSdnSanctionsAdapter(OfacSdnProperties properties) {
        this.properties = properties;

        var httpClient = HttpClient.newBuilder()
                .version(Version.HTTP_1_1)
                .connectTimeout(Duration.ofMillis(properties.connectionTimeoutMs()))
                .build();

        var requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofMillis(properties.readTimeoutMs()));

        this.restClient = RestClient.builder()
                .baseUrl(properties.baseUrl())
                .requestFactory(requestFactory)
                .build();
    }

    @Override
    public SanctionsResult screen(UUID senderId, UUID recipientId) {
        log.info("[OFAC-SDN] Screening sender={} recipient={}", senderId, recipientId);

        var entries = getEntries();

        var senderMatches = findMatches(senderId.toString(), entries);
        var recipientMatches = findMatches(recipientId.toString(), entries);

        boolean senderHit = !senderMatches.isEmpty();
        boolean recipientHit = !recipientMatches.isEmpty();

        var hitDetails = buildHitDetails(senderMatches, recipientMatches);

        var providerRef = buildProviderRef(senderMatches, recipientMatches);

        var result = SanctionsResult.builder()
                .sanctionsResultId(UUID.randomUUID())
                .senderScreened(true)
                .recipientScreened(true)
                .senderHit(senderHit)
                .recipientHit(recipientHit)
                .hitDetails(hitDetails)
                .listsChecked(LISTS_CHECKED)
                .provider(PROVIDER_NAME)
                .providerRef(providerRef)
                .screenedAt(Instant.now())
                .build();

        if (senderHit || recipientHit) {
            log.warn("[OFAC-SDN] Sanctions HIT detected sender={} senderHit={} recipient={} recipientHit={}",
                    senderId, senderHit, recipientId, recipientHit);
        } else {
            log.info("[OFAC-SDN] No sanctions hits sender={} recipient={}", senderId, recipientId);
        }

        return result;
    }

    List<SdnMatchResult> findMatches(String name, List<SdnEntry> entries) {
        var normalizedInput = JaroWinklerSimilarity.normalize(name);
        var matches = new ArrayList<SdnMatchResult>();

        for (var entry : entries) {
            findBestMatch(normalizedInput, entry).ifPresent(matches::add);
        }

        return List.copyOf(matches);
    }

    private Optional<SdnMatchResult> findBestMatch(String normalizedInput, SdnEntry entry) {
        double bestScore = 0.0;
        String bestName = null;

        for (var entryName : entry.allNames()) {
            double score = JaroWinklerSimilarity.similarity(normalizedInput, entryName);
            if (score > bestScore) {
                bestScore = score;
                bestName = entryName;
            }
        }

        if (bestScore >= properties.matchThreshold()) {
            return Optional.of(new SdnMatchResult(entry.uid(), bestName, entry.sdnType(), bestScore));
        }

        return Optional.empty();
    }

    private List<SdnEntry> getEntries() {
        cacheLock.readLock().lock();
        try {
            if (cachedEntries != null && !isCacheExpired()) {
                return cachedEntries;
            }
        } finally {
            cacheLock.readLock().unlock();
        }

        cacheLock.writeLock().lock();
        try {
            if (cachedEntries != null && !isCacheExpired()) {
                return cachedEntries;
            }
            refreshSdnList();
            return cachedEntries;
        } finally {
            cacheLock.writeLock().unlock();
        }
    }

    private boolean isCacheExpired() {
        if (lastRefresh == null) {
            return true;
        }
        return Duration.between(lastRefresh, Instant.now()).toHours() >= properties.cacheRefreshHours();
    }

    @Retry(name = "sanctions", fallbackMethod = "downloadFallback")
    @CircuitBreaker(name = "sanctions", fallbackMethod = "downloadFallback")
    void refreshSdnList() {
        log.info("[OFAC-SDN] Downloading SDN list from {}/{}", properties.baseUrl(), properties.sdnFileName());

        var xml = restClient.get()
                .uri("/{fileName}", properties.sdnFileName())
                .retrieve()
                .body(String.class);

        var entries = SdnXmlParser.parse(xml);
        this.cachedEntries = entries;
        this.lastRefresh = Instant.now();

        log.info("[OFAC-SDN] SDN list loaded with {} entries", entries.size());
    }

    @SuppressWarnings("unused")
    private void downloadFallback(Exception ex) {
        log.error("[OFAC-SDN] Failed to download SDN list: {}", ex.getMessage(), ex);
        if (cachedEntries == null) {
            throw new IllegalStateException("OFAC SDN list unavailable and no cached data", ex);
        }
        log.warn("[OFAC-SDN] Using stale cached SDN list ({} entries, last refresh: {})",
                cachedEntries.size(), lastRefresh);
    }

    private String buildHitDetails(List<SdnMatchResult> senderMatches, List<SdnMatchResult> recipientMatches) {
        if (senderMatches.isEmpty() && recipientMatches.isEmpty()) {
            return null;
        }

        var details = new StringBuilder("{");
        boolean needsSeparator = false;

        if (!senderMatches.isEmpty()) {
            details.append("\"senderMatches\":[");
            details.append(formatMatches(senderMatches));
            details.append("]");
            needsSeparator = true;
        }

        if (!recipientMatches.isEmpty()) {
            if (needsSeparator) {
                details.append(",");
            }
            details.append("\"recipientMatches\":[");
            details.append(formatMatches(recipientMatches));
            details.append("]");
        }

        details.append("}");
        return details.toString();
    }

    private String formatMatches(List<SdnMatchResult> matches) {
        return matches.stream()
                .map(m -> "{\"entryUid\":%d,\"sdnType\":\"%s\",\"score\":%.4f}"
                        .formatted(m.entryUid(), m.sdnType(), m.score()))
                .reduce((a, b) -> a + "," + b)
                .orElse("");
    }

    private String buildProviderRef(List<SdnMatchResult> senderMatches, List<SdnMatchResult> recipientMatches) {
        var senderRef = senderMatches.isEmpty() ? "clear" : "hit-" + senderMatches.getFirst().entryUid();
        var recipientRef = recipientMatches.isEmpty() ? "clear" : "hit-" + recipientMatches.getFirst().entryUid();
        return "ofac-sdn:%s/%s".formatted(senderRef, recipientRef);
    }
}
