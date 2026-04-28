package com.zoho.trino;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.trino.spi.StandardErrorCode;
import io.trino.spi.TrinoException;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ZohoApiClient
{
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final ZohoConfig config;
    private final HttpClient httpClient;

    public ZohoApiClient(ZohoConfig config)
    {
        this.config = config;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(config.getTimeout())
                .build();
    }

    public List<ZohoRow> fetchModuleRows(String moduleName)
    {
        java.net.URI moduleUri = config.getModuleUri(moduleName);
        HttpRequest request = HttpRequest.newBuilder(moduleUri)
                .timeout(config.getTimeout())
                .GET()
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new TrinoException(
                        StandardErrorCode.GENERIC_INTERNAL_ERROR,
                        "Zoho connector received HTTP " + response.statusCode() + " from " + moduleUri);
            }
            return parseRows(response.body());
        }
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new TrinoException(StandardErrorCode.GENERIC_INTERNAL_ERROR, "Failed to call Express API at " + moduleUri, e);
        }
        catch (IOException e) {
            if ("host.docker.internal".equalsIgnoreCase(moduleUri.getHost())) {
                java.net.URI fallbackUri = java.net.URI.create(moduleUri.toString().replaceFirst("host\\.docker\\.internal", "localhost"));
                try {
                    HttpRequest fallbackRequest = HttpRequest.newBuilder(fallbackUri)
                            .timeout(config.getTimeout())
                            .GET()
                            .build();
                    HttpResponse<String> fallbackResponse = httpClient.send(fallbackRequest, HttpResponse.BodyHandlers.ofString());
                    if (fallbackResponse.statusCode() < 200 || fallbackResponse.statusCode() >= 300) {
                        throw new TrinoException(
                                StandardErrorCode.GENERIC_INTERNAL_ERROR,
                                "Zoho connector received HTTP " + fallbackResponse.statusCode() + " from " + fallbackUri);
                    }
                    return parseRows(fallbackResponse.body());
                }
                catch (InterruptedException fallbackInterrupted) {
                    Thread.currentThread().interrupt();
                    throw new TrinoException(StandardErrorCode.GENERIC_INTERNAL_ERROR, "Failed to call Express API at " + fallbackUri, fallbackInterrupted);
                }
                catch (IOException fallbackIOException) {
                    throw new TrinoException(
                            StandardErrorCode.GENERIC_INTERNAL_ERROR,
                            "Failed to call Express API at " + moduleUri + " and fallback to " + fallbackUri,
                            fallbackIOException);
                }
            }
            throw new TrinoException(StandardErrorCode.GENERIC_INTERNAL_ERROR, "Failed to call Express API at " + moduleUri, e);
        }
    }

    private List<ZohoRow> parseRows(String body)
            throws IOException
    {
        JsonNode root = OBJECT_MAPPER.readTree(body);
        JsonNode dataNode = root.isArray() ? root : root.path("data");

        if (!dataNode.isArray()) {
            throw new TrinoException(
                    StandardErrorCode.GENERIC_INTERNAL_ERROR,
                    "Express API response must contain a top-level data array");
        }

        List<ZohoRow> rows = new ArrayList<>();
        for (JsonNode item : dataNode) {
            LinkedHashMap<String, String> values = new LinkedHashMap<>();
            if (item.isObject()) {
                Iterator<Map.Entry<String, JsonNode>> fields = item.fields();
                while (fields.hasNext()) {
                    Map.Entry<String, JsonNode> field = fields.next();
                    values.put(field.getKey(), jsonValue(field.getValue()));
                }
            }
            rows.add(new ZohoRow(values, OBJECT_MAPPER.writeValueAsString(item)));
        }
        return rows;
    }

    private static String jsonValue(JsonNode node)
    {
        if (node == null || node.isNull()) {
            return null;
        }
        if (node.isContainerNode()) {
            return node.toString();
        }
        return node.asText();
    }
}
