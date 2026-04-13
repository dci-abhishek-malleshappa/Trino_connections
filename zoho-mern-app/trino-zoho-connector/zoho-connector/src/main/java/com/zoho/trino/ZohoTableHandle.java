package com.zoho.trino;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.trino.spi.connector.ConnectorTableHandle;
import io.trino.spi.connector.SchemaTableName;

import java.util.Objects;

public final class ZohoTableHandle
        implements ConnectorTableHandle
{
    private final SchemaTableName schemaTableName;
    private final String moduleName;

    @JsonCreator
    public ZohoTableHandle(
            @JsonProperty("schemaTableName") SchemaTableName schemaTableName,
            @JsonProperty("moduleName") String moduleName)
    {
        this.schemaTableName = Objects.requireNonNull(schemaTableName, "schemaTableName is null");
        this.moduleName = Objects.requireNonNull(moduleName, "moduleName is null");
    }

    @JsonProperty
    public SchemaTableName schemaTableName()
    {
        return schemaTableName;
    }

    @JsonProperty
    public String moduleName()
    {
        return moduleName;
    }

    @Override
    public boolean equals(Object other)
    {
        if (this == other) {
            return true;
        }
        if (!(other instanceof ZohoTableHandle that)) {
            return false;
        }
        return schemaTableName.equals(that.schemaTableName) && moduleName.equals(that.moduleName);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(schemaTableName, moduleName);
    }
}
