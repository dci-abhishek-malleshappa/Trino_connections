package com.example.trino.tally;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.stream.Collectors;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.requireNonNull;

public class TallyXmlClient implements TallyClient
{
    private final TallyConfig config;
    private final HttpClient httpClient;
    private final TallyXmlParser parser;

    public TallyXmlClient(TallyConfig config, HttpClient httpClient, TallyXmlParser parser)
    {
        this.config = requireNonNull(config, "config is null");
        this.httpClient = requireNonNull(httpClient, "httpClient is null");
        this.parser = requireNonNull(parser, "parser is null");
    }

    @Override
    public List<TallyRow> getRows(TallyTableDefinition tableDefinition)
    {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(config.getEndpoint()))
                .header("Content-Type", "application/xml; charset=utf-8")
                .POST(HttpRequest.BodyPublishers.ofString(buildRequest(tableDefinition), UTF_8))
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(UTF_8));
            String body = response.body();

            try {
                List<TallyRow> rows = parser.parseRows(body, tableDefinition);
                System.out.println("[tally] table=" + tableDefinition.getTableName() + ", endpoint=" + config.getEndpoint() + ", status=" + response.statusCode() + ", parsedRows=" + rows.size());
                System.out.println("[tally] responseSnippet=" + sanitizeForLog(body));
                rows.stream()
                        .limit(5)
                        .forEach(row -> System.out.println("[tally] row=" + row));
                return rows;
            }
            catch (RuntimeException e) {
                System.out.println("[tally] table=" + tableDefinition.getTableName() + ", endpoint=" + config.getEndpoint() + ", status=" + response.statusCode() + ", parseFailed=true");
                System.out.println("[tally] responseSnippet=" + sanitizeForLog(body));
                throw e;
            }
        }
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while calling Tally", e);
        }
        catch (IOException e) {
            throw new UncheckedIOException("Failed to call Tally endpoint", e);
        }
    }

    private String buildRequest(TallyTableDefinition tableDefinition)
    {
        String collectionName = "Trino" + toPascalCase(tableDefinition.getTableName());
        String nativeMethods = tableDefinition.getColumns().stream()
                .map(TallyColumnDefinition::nativeMethod)
                .distinct()
                .map(method -> "                        <NATIVEMETHOD>%s</NATIVEMETHOD>".formatted(escapeXml(method)))
                .collect(Collectors.joining(System.lineSeparator()));

        return """
                <ENVELOPE>
                  <HEADER>
                    <VERSION>1</VERSION>
                    <TALLYREQUEST>Export</TALLYREQUEST>
                    <TYPE>Collection</TYPE>
                    <ID>%s</ID>
                  </HEADER>
                  <BODY>
                    <DESC>
                      <STATICVARIABLES>
                        <SVCURRENTCOMPANY>%s</SVCURRENTCOMPANY>
                      </STATICVARIABLES>
                      <TDL>
                        <TDLMESSAGE>
                          <COLLECTION NAME=\"%s\" ISMODIFY=\"No\">
                            <TYPE>%s</TYPE>
%s
                          </COLLECTION>
                        </TDLMESSAGE>
                      </TDL>
                    </DESC>
                  </BODY>
                </ENVELOPE>
                """.formatted(
                collectionName,
                escapeXml(config.getCompany()),
                collectionName,
                escapeXml(tableDefinition.getTallyType()),
                nativeMethods);
    }

    private static String sanitizeForLog(String body)
    {
        String flattened = body.replaceAll("\\s+", " ").trim();
        if (flattened.length() > 500) {
            return flattened.substring(0, 500) + "...";
        }
        return flattened;
    }

    private static String escapeXml(String value)
    {
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }

    private static String toPascalCase(String value)
    {
        StringBuilder builder = new StringBuilder();
        for (String part : value.split("[_\\s]+")) {
            if (part.isEmpty()) {
                continue;
            }
            builder.append(Character.toUpperCase(part.charAt(0)));
            if (part.length() > 1) {
                builder.append(part.substring(1));
            }
        }
        return builder.toString();
    }
}
