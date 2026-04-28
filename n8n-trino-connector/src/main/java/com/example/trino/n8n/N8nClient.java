package com.example.trino.n8n;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.OptionalInt;
import java.util.stream.Stream;

public class N8nClient
{
    private final HttpClient httpClient;
    private final String baseUrl;
    private final String endpoint;
    private final String fallbackEndpoint;
    private final String authToken;
    private final int readTimeoutMs;
    private final int pageSize;
    private final String dataMethod;
    private final String directSchemaName;
    private final String directTableName;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public N8nClient(N8nConfig config)
    {
        this.baseUrl = config.getBaseUrl();
        this.endpoint = config.getEndpoint();
        this.fallbackEndpoint = config.getFallbackEndpoint();
        this.authToken = config.getAuthToken();
        this.readTimeoutMs = config.getReadTimeoutMs();
        this.pageSize = config.getPageSize();
        this.dataMethod = config.getDataMethod();
        this.directSchemaName = config.getDirectSchemaName();
        this.directTableName = config.getDirectTableName();
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofMillis(config.getConnectTimeoutMs()))
                .build();
    }

    public List<String> getSchemas()
    {
        if (isDirectEndpointMode()) {
            return List.of(directSchemaName);
        }
        JsonNode response = getJson("/meta/schemas");
        JsonNode schemas = response.get("schemas");
        if (schemas == null || !schemas.isArray()) {
            throw new RuntimeException("Invalid schemas response: " + response);
        }
        return objectMapper.convertValue(schemas, List.class);
    }

    public List<String> getTables(String schema)
    {
        if (isDirectEndpointMode()) {
            if (!directSchemaName.equals(schema)) {
                return List.of();
            }
            return detectDirectTables(getDirectPayload());
        }
        JsonNode response = getJson(buildPath("/meta/tables", Map.of("schema", schema)));
        JsonNode tables = response.get("tables");
        if (tables == null || !tables.isArray()) {
            throw new RuntimeException("Invalid tables response for schema " + schema + ": " + response);
        }
        return parseTableNames(schema, tables);
    }

    public N8nTableDefinition getTableDefinition(String schema, String table)
    {
        if (isDirectEndpointMode()) {
            return inferDirectTableDefinition(schema, table, getDirectRows(table));
        }
        JsonNode response = getJson(buildPath("/meta/table", Map.of(
                "schema", schema,
                "table", table)));
        return objectMapper.convertValue(response, N8nTableDefinition.class);
    }

    public List<Map<String, Object>> getData(String schema, String table)
    {
        return getDataPage(schema, table, 0).data();
    }

    public N8nDataPage getDataPage(String schema, String table, int offset)
    {
        if (isDirectEndpointMode()) {
            List<Map<String, Object>> rows = getDirectRows(table);
            int fromIndex = Math.min(offset, rows.size());
            int toIndex = Math.min(fromIndex + pageSize, rows.size());
            OptionalInt next = toIndex < rows.size() ? OptionalInt.of(toIndex) : OptionalInt.empty();
            return new N8nDataPage(rows.subList(fromIndex, toIndex), next);
        }
        JsonNode response = getJson(buildPath("/data", Map.of(
                "schema", schema,
                "table", table,
                "offset", Integer.toString(offset),
                "limit", Integer.toString(pageSize))), dataMethod);
        JsonNode data = response.get("data");
        if (data == null || !data.isArray()) {
            throw new RuntimeException("Invalid data response for " + schema + "." + table + ": " + response);
        }
        List<Map<String, Object>> rows = objectMapper.convertValue(data, List.class);
        return new N8nDataPage(rows, extractNextOffset(response, offset, rows.size()));
    }

    private JsonNode getJson(String path)
    {
        return getJson(path, "GET");
    }

    private JsonNode getJson(String path, String method)
    {
        if (isDirectEndpointMode()) {
            return getDirectPayload();
        }
        return getJsonFromAbsoluteUrl(baseUrl + path, method);
    }

    private JsonNode getDirectPayload()
    {
        RuntimeException lastError = null;
        for (String url : Stream.of(endpoint, fallbackEndpoint).filter(value -> value != null).toList()) {
            try {
                return getJsonFromAbsoluteUrl(url, "GET");
            }
            catch (RuntimeException e) {
                lastError = e;
            }
        }
        if (lastError != null) {
            throw lastError;
        }
        throw new IllegalStateException("Direct endpoint mode is enabled, but no endpoint URL is configured");
    }

    private JsonNode getJsonFromAbsoluteUrl(String url, String method)
    {
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofMillis(readTimeoutMs));

            if (method.equals("POST")) {
                builder.POST(HttpRequest.BodyPublishers.noBody());
            }
            else {
                builder.GET();
            }

            if (authToken != null) {
                builder.header("Authorization", "Bearer " + authToken);
            }

            HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new N8nHttpException(response.statusCode(), "HTTP " + response.statusCode() + " from " + url + ": " + response.body());
            }

            return objectMapper.readTree(response.body());
        }
        catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new RuntimeException("Failed to fetch " + url, e);
        }
    }

    private String buildPath(String endpoint, Map<String, String> queryParams)
    {
        if (queryParams.isEmpty()) {
            return endpoint;
        }

        String query = queryParams.entrySet().stream()
                .map(entry -> encode(entry.getKey()) + "=" + encode(entry.getValue()))
                .reduce((left, right) -> left + "&" + right)
                .orElse("");
        return endpoint + "?" + query;
    }

    private String encode(String value)
    {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private boolean isDirectEndpointMode()
    {
        return endpoint != null || fallbackEndpoint != null;
    }

    private List<String> parseTableNames(String schema, JsonNode tables)
    {
        List<String> tableNames = new ArrayList<>();
        for (JsonNode table : tables) {
            if (table.isTextual()) {
                tableNames.add(table.asText());
            }
            else if (table.isObject()) {
                JsonNode name = table.get("name");
                if (name == null) {
                    name = table.get("table");
                }
                if (name == null || !name.isTextual()) {
                    throw new RuntimeException("Invalid table entry for schema " + schema + ": " + table);
                }
                tableNames.add(name.asText());
            }
            else {
                throw new RuntimeException("Invalid table entry for schema " + schema + ": " + table);
            }
        }
        return tableNames;
    }

    private List<String> detectDirectTables(JsonNode payload)
    {
        if (directTableName != null) {
            return List.of(directTableName);
        }

        if (payload.isArray()) {
            return List.of("items");
        }

        if (payload.isObject()) {
            List<String> tables = new ArrayList<>();
            payload.fields().forEachRemaining(entry -> {
                if (entry.getValue().isArray()) {
                    tables.add(entry.getKey());
                }
            });
            if (!tables.isEmpty()) {
                return tables;
            }
        }

        return List.of("items");
    }

    private N8nTableDefinition inferDirectTableDefinition(String schema, String table, List<Map<String, Object>> rows)
    {
        N8nTableDefinition definition = new N8nTableDefinition();
        definition.setSchema(schema);
        definition.setTable(table);

        Map<String, String> columnTypes = new LinkedHashMap<>();
        for (Map<String, Object> row : rows) {
            for (Map.Entry<String, Object> entry : row.entrySet()) {
                if (!columnTypes.containsKey(entry.getKey())) {
                    columnTypes.put(entry.getKey(), inferType(entry.getValue()));
                }
            }
        }

        if (columnTypes.isEmpty()) {
            columnTypes.put("value", "varchar");
        }

        List<N8nColumn> columns = columnTypes.entrySet().stream()
                .map(entry -> {
                    N8nColumn column = new N8nColumn();
                    column.setName(entry.getKey());
                    column.setType(entry.getValue());
                    return column;
                })
                .toList();
        definition.setColumns(columns);
        return definition;
    }

    private List<Map<String, Object>> getDirectRows(String table)
    {
        JsonNode payload = getDirectPayload();
        JsonNode tableNode = selectDirectTableNode(payload, table);

        if (tableNode.isArray()) {
            List<Map<String, Object>> rows = new ArrayList<>();
            for (JsonNode node : tableNode) {
                rows.add(convertRow(node));
            }
            return rows;
        }

        if (tableNode.isObject()) {
            return List.of(convertRow(tableNode));
        }

        return List.of(Map.of("value", jsonValueToJava(tableNode)));
    }

    private JsonNode selectDirectTableNode(JsonNode payload, String table)
    {
        if (payload.isArray()) {
            return payload;
        }

        if (payload.isObject()) {
            if (payload.has(table) && payload.get(table).isArray()) {
                return payload.get(table);
            }
            if (payload.has("data") && payload.get("data").isArray()) {
                return payload.get("data");
            }

            List<String> tables = detectDirectTables(payload);
            if (!tables.isEmpty() && !tables.contains(table)) {
                throw new N8nHttpException(404, "Table not found in direct endpoint payload: " + table);
            }
            if (!tables.isEmpty() && payload.has(tables.get(0)) && payload.get(tables.get(0)).isArray()) {
                return payload.get(tables.get(0));
            }
            return payload;
        }

        return payload;
    }

    private Map<String, Object> convertRow(JsonNode node)
    {
        if (node.isObject()) {
            Map<String, Object> row = new LinkedHashMap<>();
            node.fields().forEachRemaining(entry -> row.put(entry.getKey(), jsonValueToJava(entry.getValue())));
            return row;
        }
        return Map.of("value", jsonValueToJava(node));
    }

    private Object jsonValueToJava(JsonNode node)
    {
        if (node == null || node.isNull()) {
            return null;
        }
        if (node.isTextual()) {
            return node.asText();
        }
        if (node.isBoolean()) {
            return node.asBoolean();
        }
        if (node.isIntegralNumber()) {
            return node.asLong();
        }
        if (node.isFloatingPointNumber()) {
            return node.asDouble();
        }
        if (node.isObject() || node.isArray()) {
            return node.toString();
        }
        return node.asText();
    }

    private String inferType(Object value)
    {
        if (value == null) {
            return "varchar";
        }
        if (value instanceof Boolean) {
            return "boolean";
        }
        if (value instanceof Integer) {
            return "integer";
        }
        if (value instanceof Long) {
            return "bigint";
        }
        if (value instanceof Float || value instanceof Double) {
            return "double";
        }
        if (value instanceof String textValue) {
            if (looksLikeDate(textValue)) {
                return "date";
            }
            if (looksLikeTimestamp(textValue)) {
                return "timestamp";
            }
        }
        return "varchar";
    }

    private boolean looksLikeDate(String value)
    {
        try {
            LocalDate.parse(value);
            return true;
        }
        catch (Exception ignored) {
            return false;
        }
    }

    private boolean looksLikeTimestamp(String value)
    {
        try {
            OffsetDateTime.parse(value);
            return true;
        }
        catch (Exception ignored) {
            try {
                LocalDateTime.parse(value);
                return true;
            }
            catch (Exception ignoredAgain) {
                return false;
            }
        }
    }

    public static class N8nHttpException
            extends RuntimeException
    {
        private final int statusCode;

        public N8nHttpException(int statusCode, String message)
        {
            super(message);
            this.statusCode = statusCode;
        }

        public int getStatusCode()
        {
            return statusCode;
        }
    }

    public record N8nDataPage(List<Map<String, Object>> data, OptionalInt nextOffset)
    {
    }

    private OptionalInt extractNextOffset(JsonNode response, int currentOffset, int rowsFetched)
    {
        JsonNode nextOffsetNode = response.get("nextOffset");
        if (nextOffsetNode != null && nextOffsetNode.isInt()) {
            return OptionalInt.of(nextOffsetNode.asInt());
        }

        JsonNode hasMoreNode = response.get("hasMore");
        if (hasMoreNode != null && hasMoreNode.isBoolean()) {
            if (hasMoreNode.asBoolean()) {
                return OptionalInt.of(currentOffset + rowsFetched);
            }
            return OptionalInt.empty();
        }

        return OptionalInt.empty();
    }

    public static class N8nTableDefinition
    {
        private String schema;
        private String table;
        private List<N8nColumn> columns;

        public String getSchema()
        {
            return schema;
        }

        public void setSchema(String schema)
        {
            this.schema = schema;
        }

        public String getTable()
        {
            return table;
        }

        public void setTable(String table)
        {
            this.table = table;
        }

        public List<N8nColumn> getColumns()
        {
            return columns;
        }

        public void setColumns(List<N8nColumn> columns)
        {
            this.columns = columns;
        }
    }

    public static class N8nColumn
    {
        private String name;
        private String type;

        public String getName()
        {
            return name;
        }

        public void setName(String name)
        {
            this.name = name;
        }

        public String getType()
        {
            return type;
        }

        public void setType(String type)
        {
            this.type = type;
        }
    }
}
