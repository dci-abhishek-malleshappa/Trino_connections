package com.example.trino.tally;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class TallyXmlParser
{
    public List<TallyRow> parseRows(String xml, TallyTableDefinition tableDefinition)
    {
        try {
            String sanitizedXml = sanitizeXml(xml);

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(false);
            factory.setExpandEntityReferences(false);

            Document document = factory.newDocumentBuilder().parse(new InputSource(new StringReader(sanitizedXml)));
            NodeList nodes = document.getElementsByTagName(tableDefinition.getRowTagName());
            List<TallyRow> rows = new ArrayList<>();

            for (int i = 0; i < nodes.getLength(); i++) {
                Node node = nodes.item(i);
                if (!(node instanceof Element element)) {
                    continue;
                }

                TallyRow row = parseRow(element, tableDefinition);
                if (row != null) {
                    rows.add(row);
                }
            }

            return rows;
        }
        catch (Exception e) {
            throw new RuntimeException("Failed to parse Tally XML response", e);
        }
    }

    private static TallyRow parseRow(Element element, TallyTableDefinition tableDefinition)
    {
        Map<String, Object> values = new LinkedHashMap<>();
        boolean anyNonNull = false;

        for (TallyColumnDefinition column : tableDefinition.getColumns()) {
            Object value = parseColumnValue(element, column);
            if (value != null) {
                anyNonNull = true;
            }
            values.put(column.columnName(), value);
        }

        if (!anyNonNull) {
            return null;
        }
        return new TallyRow(values);
    }

    private static Object parseColumnValue(Element element, TallyColumnDefinition column)
    {
        String value = extractValue(element, column.sourceNames());
        return switch (column.typeName()) {
            case "varchar" -> value;
            case "double" -> parseDouble(value);
            default -> throw new IllegalArgumentException("Unsupported type: " + column.typeName());
        };
    }

    private static String extractValue(Element element, List<String> sourceNames)
    {
        for (String sourceName : sourceNames) {
            String value = firstNonBlank(
                    attributeOf(element, sourceName),
                    directTextOf(element, sourceName),
                    nestedTextOf(element, sourceName));
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private static String sanitizeXml(String xml)
    {
        String sanitized = xml;

        sanitized = sanitized.replaceAll("&#x0*[0-8BCEFbcef];", "");
        sanitized = sanitized.replaceAll("&#0*([0-8]|11|12|14|15|16|17|18|19|2[0-9]|30|31);", "");

        StringBuilder builder = new StringBuilder(sanitized.length());
        for (int i = 0; i < sanitized.length(); i++) {
            char ch = sanitized.charAt(i);
            if (isAllowedXmlChar(ch)) {
                builder.append(ch);
            }
        }
        return builder.toString();
    }

    private static boolean isAllowedXmlChar(char ch)
    {
        return ch == 0x9 ||
                ch == 0xA ||
                ch == 0xD ||
                (ch >= 0x20 && ch <= 0xD7FF) ||
                (ch >= 0xE000 && ch <= 0xFFFD);
    }

    private static String attributeOf(Element element, String attributeName)
    {
        if (!element.hasAttribute(attributeName)) {
            return null;
        }
        return firstNonBlank(element.getAttribute(attributeName));
    }

    private static String directTextOf(Element parent, String tagName)
    {
        NodeList childNodes = parent.getChildNodes();
        for (int i = 0; i < childNodes.getLength(); i++) {
            Node child = childNodes.item(i);
            if (child instanceof Element childElement && tagName.equals(childElement.getTagName())) {
                return firstNonBlank(childElement.getTextContent());
            }
        }
        return null;
    }

    private static String nestedTextOf(Element parent, String tagName)
    {
        NodeList nodes = parent.getElementsByTagName(tagName);
        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            if (node instanceof Element element) {
                return firstNonBlank(element.getTextContent());
            }
        }
        return null;
    }

    private static String firstNonBlank(String... values)
    {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }

    private static Double parseDouble(String value)
    {
        if (value == null || value.isBlank()) {
            return null;
        }

        String normalized = value.trim()
                .replace(",", "")
                .replace("Dr", "")
                .replace("Cr", "")
                .trim();

        if (normalized.isEmpty()) {
            return null;
        }
        return Double.parseDouble(normalized);
    }
}
