package com.zoho.trino;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static java.lang.Integer.parseInt;

public final class ZohoConfig
{
    private static final String DEFAULT_BASE_URL = "http://host.docker.internal:5001/api/zoho";
    private static final String DEFAULT_SCHEMA = "zoho";
    private static final String DEFAULT_TABLES = "contacts,items,invoices";
    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(30);

    private final URI baseUri;
    private final String schemaName;
    private final Duration timeout;
    private final List<String> tableNames;
    private final Optional<String> organizationId;

    public ZohoConfig(URI baseUri, String schemaName, Duration timeout, List<String> tableNames, Optional<String> organizationId)
    {
        this.baseUri = Objects.requireNonNull(baseUri, "baseUri is null");
        this.schemaName = Objects.requireNonNull(schemaName, "schemaName is null");
        this.timeout = Objects.requireNonNull(timeout, "timeout is null");
        this.tableNames = List.copyOf(Objects.requireNonNull(tableNames, "tableNames is null"));
        this.organizationId = Objects.requireNonNull(organizationId, "organizationId is null");
    }

    public static ZohoConfig from(Map<String, String> config)
    {
        Objects.requireNonNull(config, "config is null");

        String rawBaseUrl = config.getOrDefault("zoho.base-url", DEFAULT_BASE_URL);
        String schemaName = config.getOrDefault("zoho.schema-name", DEFAULT_SCHEMA);
        String timeoutValue = config.getOrDefault("zoho.http-timeout-seconds", Long.toString(DEFAULT_TIMEOUT.getSeconds()));
        String rawTables = config.getOrDefault("zoho.tables", DEFAULT_TABLES);
        Optional<String> organizationId = Optional.ofNullable(config.get("zoho.organization-id"))
                .map(String::trim)
                .filter(value -> !value.isBlank());

        if (schemaName.isBlank()) {
            throw new IllegalArgumentException("zoho.schema-name must not be blank");
        }

        List<String> tableNames = rawTables.lines()
                .flatMap(line -> Arrays.stream(line.split(",")))
                .map(String::trim)
                .map(String::toLowerCase)
                .filter(value -> !value.isBlank())
                .distinct()
                .toList();

        if (tableNames.isEmpty()) {
            throw new IllegalArgumentException("zoho.tables must contain at least one table name");
        }

        try {
            URI uri = new URI(rawBaseUrl);
            if (uri.getScheme() == null || uri.getHost() == null) {
                throw new IllegalArgumentException("zoho.base-url must be a valid absolute URI");
            }
            return new ZohoConfig(uri, schemaName, Duration.ofSeconds(parseInt(timeoutValue)), tableNames, organizationId);
        }
        catch (URISyntaxException e) {
            throw new IllegalArgumentException("zoho.base-url is invalid: " + rawBaseUrl, e);
        }
    }

    public URI getBaseUri()
    {
        return baseUri;
    }

    public String getSchemaName()
    {
        return schemaName;
    }

    public Duration getTimeout()
    {
        return timeout;
    }

    public List<String> getTableNames()
    {
        return tableNames;
    }

    public Optional<String> getOrganizationId()
    {
        return organizationId;
    }

    public URI getModuleUri(String moduleName)
    {
        String normalizedBase = baseUri.toString().endsWith("/") ? baseUri.toString() : baseUri.toString() + "/";
        String uri = normalizedBase + moduleName;
        if (organizationId.isPresent()) {
            uri = uri + "?organization_id=" + organizationId.get();
        }
        return URI.create(uri);
    }
}
