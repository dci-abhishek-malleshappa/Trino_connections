package com.example.trino.n8n;

import java.util.Map;

public class N8nConfig
{
    private final String baseUrl;
    private final String endpoint;
    private final String fallbackEndpoint;
    private final String authToken;
    private final int connectTimeoutMs;
    private final int readTimeoutMs;
    private final int pageSize;
    private final String dataMethod;
    private final String directSchemaName;
    private final String directTableName;

    public N8nConfig(Map<String, String> config)
    {
        this.baseUrl = normalizeUrl(config.get("n8n.base-url"));
        this.endpoint = normalizeUrl(config.get("endpoint"));
        this.fallbackEndpoint = normalizeUrl(config.get("fallback-endpoint"));

        if (baseUrl == null && endpoint == null && fallbackEndpoint == null) {
            throw new IllegalArgumentException("One of n8n.base-url, endpoint, or fallback-endpoint is required");
        }
        this.authToken = config.get("n8n.auth-token");
        this.connectTimeoutMs = parsePositiveInt(config, "n8n.connect-timeout-ms", "5000");
        this.readTimeoutMs = parsePositiveInt(config, "n8n.read-timeout-ms", "30000");
        this.pageSize = parsePositiveInt(config, "n8n.page-size", "1000");
        this.dataMethod = parseHttpMethod(config.getOrDefault("n8n.data-method", "GET"));
        this.directSchemaName = config.getOrDefault("n8n.schema-name", "books");
        this.directTableName = emptyToNull(config.get("n8n.table-name"));
    }

    public String getBaseUrl()
    {
        return baseUrl;
    }

    public boolean isDirectEndpointMode()
    {
        return endpoint != null || fallbackEndpoint != null;
    }

    public String getEndpoint()
    {
        return endpoint;
    }

    public String getFallbackEndpoint()
    {
        return fallbackEndpoint;
    }

    public String getAuthToken()
    {
        return authToken;
    }

    public int getConnectTimeoutMs()
    {
        return connectTimeoutMs;
    }

    public int getReadTimeoutMs()
    {
        return readTimeoutMs;
    }

    public int getPageSize()
    {
        return pageSize;
    }

    public String getDataMethod()
    {
        return dataMethod;
    }

    public String getDirectSchemaName()
    {
        return directSchemaName;
    }

    public String getDirectTableName()
    {
        return directTableName;
    }

    private static int parsePositiveInt(Map<String, String> config, String key, String defaultValue)
    {
        int parsedValue = Integer.parseInt(config.getOrDefault(key, defaultValue));
        if (parsedValue <= 0) {
            throw new IllegalArgumentException(key + " must be greater than 0");
        }
        return parsedValue;
    }

    private static String normalizeUrl(String value)
    {
        String normalized = emptyToNull(value);
        if (normalized == null) {
            return null;
        }
        return normalized.endsWith("/") ? normalized.substring(0, normalized.length() - 1) : normalized;
    }

    private static String parseHttpMethod(String value)
    {
        String method = value.trim().toUpperCase();
        if (!method.equals("GET") && !method.equals("POST")) {
            throw new IllegalArgumentException("n8n.data-method must be GET or POST");
        }
        return method;
    }

    private static String emptyToNull(String value)
    {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value;
    }
}
