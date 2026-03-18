package com.stablecoin.payments.compliance.infrastructure.provider.ofacsdn;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

/**
 * Parses OFAC SDN XML into {@link SdnEntry} records using StAX streaming parser
 * for memory-efficient processing of large XML files.
 */
final class SdnXmlParser {

    private SdnXmlParser() {
    }

    static List<SdnEntry> parse(String xml) {
        if (xml == null || xml.isBlank()) {
            return List.of();
        }

        var entries = new ArrayList<SdnEntry>();
        var factory = XMLInputFactory.newInstance();
        factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
        factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);

        try {
            var reader = factory.createXMLStreamReader(new StringReader(xml));
            parseDocument(reader, entries);
            reader.close();
        } catch (XMLStreamException e) {
            throw new SdnParseException("Failed to parse SDN XML", e);
        }

        return List.copyOf(entries);
    }

    private static void parseDocument(XMLStreamReader reader, List<SdnEntry> entries)
            throws XMLStreamException {
        while (reader.hasNext()) {
            int event = reader.next();
            if (event == XMLStreamConstants.START_ELEMENT && "sdnEntry".equals(reader.getLocalName())) {
                var entry = parseSdnEntry(reader);
                if (entry != null) {
                    entries.add(entry);
                }
            }
        }
    }

    private static SdnEntry parseSdnEntry(XMLStreamReader reader) throws XMLStreamException {
        int uid = 0;
        String firstName = null;
        String lastName = null;
        String sdnType = null;
        var programs = new ArrayList<String>();
        var aliases = new ArrayList<SdnAlias>();

        while (reader.hasNext()) {
            int event = reader.next();

            if (event == XMLStreamConstants.START_ELEMENT) {
                switch (reader.getLocalName()) {
                    case "uid" -> uid = Integer.parseInt(readText(reader));
                    case "firstName" -> firstName = readText(reader);
                    case "lastName" -> lastName = readText(reader);
                    case "sdnType" -> sdnType = readText(reader);
                    case "program" -> programs.add(readText(reader));
                    case "aka" -> {
                        var alias = parseAlias(reader);
                        if (alias != null) {
                            aliases.add(alias);
                        }
                    }
                    default -> { /* skip unknown elements */ }
                }
            } else if (event == XMLStreamConstants.END_ELEMENT && "sdnEntry".equals(reader.getLocalName())) {
                break;
            }
        }

        return new SdnEntry(uid, firstName, lastName, sdnType, List.copyOf(programs), List.copyOf(aliases));
    }

    private static SdnAlias parseAlias(XMLStreamReader reader) throws XMLStreamException {
        int uid = 0;
        String type = null;
        String category = null;
        String firstName = null;
        String lastName = null;

        while (reader.hasNext()) {
            int event = reader.next();

            if (event == XMLStreamConstants.START_ELEMENT) {
                switch (reader.getLocalName()) {
                    case "uid" -> uid = Integer.parseInt(readText(reader));
                    case "type" -> type = readText(reader);
                    case "category" -> category = readText(reader);
                    case "firstName" -> firstName = readText(reader);
                    case "lastName" -> lastName = readText(reader);
                    default -> { /* skip unknown elements */ }
                }
            } else if (event == XMLStreamConstants.END_ELEMENT && "aka".equals(reader.getLocalName())) {
                break;
            }
        }

        return new SdnAlias(uid, type, category, firstName, lastName);
    }

    private static String readText(XMLStreamReader reader) throws XMLStreamException {
        var sb = new StringBuilder();
        while (reader.hasNext()) {
            int event = reader.next();
            if (event == XMLStreamConstants.CHARACTERS || event == XMLStreamConstants.CDATA) {
                sb.append(reader.getText());
            } else if (event == XMLStreamConstants.END_ELEMENT) {
                break;
            }
        }
        return sb.toString().trim();
    }

    static class SdnParseException extends RuntimeException {
        SdnParseException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
