package com.stablecoin.payments.compliance.infrastructure.provider.ofacsdn;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SdnXmlParserTest {

    @Nested
    @DisplayName("Parsing valid XML")
    class ValidXml {

        @Test
        @DisplayName("should parse sample SDN XML with all entries")
        void parseSampleXml() throws IOException {
            var xml = loadResource("ofac/sample-sdn.xml");

            var entries = SdnXmlParser.parse(xml);

            assertThat(entries).hasSize(3);
        }

        @Test
        @DisplayName("should parse individual entry with first and last name")
        void parseIndividualEntry() throws IOException {
            var xml = loadResource("ofac/sample-sdn.xml");

            var entries = SdnXmlParser.parse(xml);
            var individual = entries.stream()
                    .filter(e -> e.uid() == 1001)
                    .findFirst()
                    .orElseThrow();

            var expected = new SdnEntry(
                    1001,
                    "Viktor",
                    "PETROV",
                    "Individual",
                    List.of("UKRAINE-EO13662"),
                    List.of(new SdnAlias(2001, "a.k.a.", "strong", "Victor", "PETROFF"))
            );
            assertThat(individual)
                    .usingRecursiveComparison()
                    .isEqualTo(expected);
        }

        @Test
        @DisplayName("should parse entity entry with last name only")
        void parseEntityEntry() throws IOException {
            var xml = loadResource("ofac/sample-sdn.xml");

            var entries = SdnXmlParser.parse(xml);
            var entity = entries.stream()
                    .filter(e -> e.uid() == 1002)
                    .findFirst()
                    .orElseThrow();

            var expected = new SdnEntry(
                    1002,
                    null,
                    "OCEANUS TRADING LTD",
                    "Entity",
                    List.of("SDGT", "IRAN"),
                    List.of()
            );
            assertThat(entity)
                    .usingRecursiveComparison()
                    .isEqualTo(expected);
        }

        @Test
        @DisplayName("should parse entry with multiple aliases")
        void parseMultipleAliases() throws IOException {
            var xml = loadResource("ofac/sample-sdn.xml");

            var entries = SdnXmlParser.parse(xml);
            var entry = entries.stream()
                    .filter(e -> e.uid() == 1003)
                    .findFirst()
                    .orElseThrow();

            assertThat(entry.aliases()).hasSize(2);
            assertThat(entry.allNames()).hasSize(3);
        }
    }

    @Nested
    @DisplayName("Edge cases")
    class EdgeCases {

        @Test
        @DisplayName("should return empty list for empty XML")
        void emptyXml() throws IOException {
            var xml = loadResource("ofac/empty-sdn.xml");

            var entries = SdnXmlParser.parse(xml);

            assertThat(entries).isEmpty();
        }

        @Test
        @DisplayName("should return empty list for null input")
        void nullInput() {
            var entries = SdnXmlParser.parse(null);

            assertThat(entries).isEmpty();
        }

        @Test
        @DisplayName("should return empty list for blank input")
        void blankInput() {
            var entries = SdnXmlParser.parse("   ");

            assertThat(entries).isEmpty();
        }
    }

    private String loadResource(String path) throws IOException {
        try (var stream = getClass().getClassLoader().getResourceAsStream(path)) {
            if (stream == null) {
                throw new IOException("Resource not found: " + path);
            }
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
