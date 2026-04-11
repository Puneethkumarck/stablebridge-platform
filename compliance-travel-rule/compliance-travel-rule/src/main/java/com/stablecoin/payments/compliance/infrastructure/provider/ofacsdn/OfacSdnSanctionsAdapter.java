package com.stablecoin.payments.compliance.infrastructure.provider.ofacsdn;

import com.stablecoin.payments.compliance.domain.model.SanctionsResult;
import com.stablecoin.payments.compliance.domain.port.SanctionsProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

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
    private final SdnListDownloader downloader;
    private final ReentrantReadWriteLock cacheLock = new ReentrantReadWriteLock();

    private volatile List<SdnEntry> cachedEntries;
    private volatile Instant lastRefresh;

    public OfacSdnSanctionsAdapter(OfacSdnProperties properties, SdnListDownloader downloader) {
        this.properties = properties;
        this.downloader = downloader;
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
            cachedEntries = downloader.download();
            lastRefresh = Instant.now();
            return cachedEntries;
        } catch (IllegalStateException ex) {
            if (cachedEntries != null) {
                log.warn("[OFAC-SDN] Using stale cached SDN list ({} entries, last refresh: {})",
                        cachedEntries.size(), lastRefresh);
                return cachedEntries;
            }
            throw ex;
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
