package com.stablecoin.payments.compliance.infrastructure.provider.ofacsdn;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.stablecoin.payments.compliance.domain.model.SanctionsResult;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OfacSdnSanctionsAdapterTest {

    private static WireMockServer wireMock;

    @BeforeAll
    static void startWireMock() {
        wireMock = new WireMockServer(wireMockConfig().dynamicPort());
        wireMock.start();
    }

    @AfterAll
    static void stopWireMock() {
        wireMock.stop();
    }

    @BeforeEach
    void setUp() {
        wireMock.resetAll();
    }

    private OfacSdnSanctionsAdapter createAdapter(double threshold) {
        var properties = new OfacSdnProperties(
                wireMock.baseUrl(),
                "sdn.xml",
                threshold,
                24,
                10000,
                30000
        );
        var downloader = new SdnListDownloader(properties);
        return new OfacSdnSanctionsAdapter(properties, downloader);
    }

    private OfacSdnSanctionsAdapter createDefaultAdapter() {
        return createAdapter(0.85);
    }

    private void stubSdnXml(String resourcePath) throws IOException {
        var xml = loadResource(resourcePath);
        wireMock.stubFor(get(urlEqualTo("/sdn.xml"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/xml")
                        .withBody(xml)));
    }

    private String loadResource(String path) throws IOException {
        try (var stream = getClass().getClassLoader().getResourceAsStream(path)) {
            if (stream == null) {
                throw new IOException("Resource not found: " + path);
            }
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    @Nested
    @DisplayName("No sanctions match")
    class NoMatch {

        @Test
        @DisplayName("should return clear result when name does not match any SDN entry")
        void noMatchReturnsClean() throws IOException {
            stubSdnXml("ofac/sample-sdn.xml");
            var adapter = createDefaultAdapter();

            var result = adapter.screen(UUID.randomUUID(), UUID.randomUUID());

            var expected = SanctionsResult.builder()
                    .senderScreened(true)
                    .recipientScreened(true)
                    .senderHit(false)
                    .recipientHit(false)
                    .hitDetails(null)
                    .listsChecked(List.of("OFAC_SDN"))
                    .provider("ofac-sdn")
                    .build();
            assertThat(result)
                    .usingRecursiveComparison()
                    .ignoringFields("sanctionsResultId", "checkId", "providerRef", "screenedAt")
                    .isEqualTo(expected);
        }
    }

    @Nested
    @DisplayName("Sanctions match")
    class Match {

        @Test
        @DisplayName("should detect sanctions hit when name matches SDN entry")
        void nameMatchReturnsHit() throws IOException {
            stubSdnXml("ofac/sample-sdn.xml");
            var adapter = createDefaultAdapter();

            var entries = SdnXmlParser.parse(loadResource("ofac/sample-sdn.xml"));
            var matches = adapter.findMatches("Viktor PETROV", entries);

            var expected = new SdnMatchResult(1001, "Viktor PETROV", "Individual", 1.0);
            assertThat(matches.getFirst())
                    .usingRecursiveComparison()
                    .isEqualTo(expected);
        }

        @Test
        @DisplayName("should return hit details with entry UID and score")
        void hitDetailsContainEntryInfo() throws IOException {
            stubSdnXml("ofac/sample-sdn.xml");
            var adapter = createDefaultAdapter();

            var entries = SdnXmlParser.parse(loadResource("ofac/sample-sdn.xml"));
            var matches = adapter.findMatches("Viktor PETROV", entries);

            var expected = new SdnMatchResult(1001, "Viktor PETROV", "Individual", 1.0);
            assertThat(matches.getFirst())
                    .usingRecursiveComparison()
                    .withComparatorForType(Double::compare, Double.class)
                    .isEqualTo(expected);
        }
    }

    @Nested
    @DisplayName("Fuzzy matching")
    class FuzzyMatch {

        @Test
        @DisplayName("should match slightly different spelling via fuzzy match")
        void fuzzySpellingMatch() throws IOException {
            stubSdnXml("ofac/sample-sdn.xml");
            var adapter = createAdapter(0.80);

            var entries = SdnXmlParser.parse(loadResource("ofac/sample-sdn.xml"));
            var matches = adapter.findMatches("Victor Petroff", entries);

            var expected = new SdnMatchResult(1001, "Viktor PETROV", "Individual", 0.0);
            assertThat(matches.getFirst())
                    .usingRecursiveComparison()
                    .ignoringFields("score", "matchedName")
                    .isEqualTo(expected);
            assertThat(matches.getFirst().score()).isGreaterThanOrEqualTo(0.80);
        }

        @Test
        @DisplayName("should match against alias names")
        void aliasMatch() throws IOException {
            stubSdnXml("ofac/sample-sdn.xml");
            var adapter = createAdapter(0.80);

            var entries = SdnXmlParser.parse(loadResource("ofac/sample-sdn.xml"));
            var matches = adapter.findMatches("Ahmed AL-RASHEED", entries);

            var expected = new SdnMatchResult(1003, "Ahmed AL-RASHEED", "Individual", 0.0);
            assertThat(matches.getFirst())
                    .usingRecursiveComparison()
                    .ignoringFields("score")
                    .isEqualTo(expected);
        }
    }

    @Nested
    @DisplayName("Threshold filtering")
    class ThresholdFiltering {

        @Test
        @DisplayName("should exclude matches below the configured threshold")
        void belowThresholdExcluded() throws IOException {
            stubSdnXml("ofac/sample-sdn.xml");
            var adapter = createAdapter(0.99);

            var entries = SdnXmlParser.parse(loadResource("ofac/sample-sdn.xml"));
            var matches = adapter.findMatches("Viktr Petrv", entries);

            assertThat(matches).isEmpty();
        }

        @Test
        @DisplayName("should include matches at or above threshold")
        void atThresholdIncluded() throws IOException {
            stubSdnXml("ofac/sample-sdn.xml");
            var adapter = createAdapter(0.50);

            var entries = SdnXmlParser.parse(loadResource("ofac/sample-sdn.xml"));
            var matches = adapter.findMatches("Victor Petroff", entries);

            assertThat(matches).isNotEmpty();
        }
    }

    @Nested
    @DisplayName("Empty SDN list")
    class EmptyList {

        @Test
        @DisplayName("should handle empty SDN list gracefully")
        void emptyListReturnsClean() throws IOException {
            stubSdnXml("ofac/empty-sdn.xml");
            var adapter = createDefaultAdapter();

            var result = adapter.screen(UUID.randomUUID(), UUID.randomUUID());

            var expected = SanctionsResult.builder()
                    .senderScreened(true)
                    .recipientScreened(true)
                    .senderHit(false)
                    .recipientHit(false)
                    .hitDetails(null)
                    .listsChecked(List.of("OFAC_SDN"))
                    .provider("ofac-sdn")
                    .build();
            assertThat(result)
                    .usingRecursiveComparison()
                    .ignoringFields("sanctionsResultId", "checkId", "providerRef", "screenedAt")
                    .isEqualTo(expected);
        }
    }

    @Nested
    @DisplayName("XML download failure")
    class DownloadFailure {

        @Test
        @DisplayName("should throw when XML download fails and no cache exists")
        void downloadFailureNoCacheThrows() {
            wireMock.stubFor(get(urlEqualTo("/sdn.xml"))
                    .willReturn(aResponse().withStatus(500)));

            var adapter = createDefaultAdapter();

            assertThatThrownBy(() -> adapter.screen(UUID.randomUUID(), UUID.randomUUID()))
                    .isInstanceOf(Exception.class);
        }
    }

    @Nested
    @DisplayName("SDN list caching")
    class Caching {

        @Test
        @DisplayName("should only download SDN list once for multiple screens")
        void cachesAfterFirstDownload() throws IOException {
            stubSdnXml("ofac/sample-sdn.xml");
            var adapter = createDefaultAdapter();

            adapter.screen(UUID.randomUUID(), UUID.randomUUID());
            adapter.screen(UUID.randomUUID(), UUID.randomUUID());

            wireMock.verify(1, getRequestedFor(urlEqualTo("/sdn.xml")));
        }
    }
}
