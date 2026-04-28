package com.example.trino.n8n;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.trino.spi.connector.ConnectorTableHandle;

import java.util.Objects;

public class N8nTableHandle
        implements ConnectorTableHandle
{
    private final String schema;
    private final String table;

    @JsonCreator
    public N8nTableHandle(
            @JsonProperty("schema") String schema,
            @JsonProperty("table") String table)
    {
        this.schema = schema;
        this.table = table;
    }

    @JsonProperty
    public String getSchema()
    {
        return schema;
    }

    @JsonProperty
    public String getTable()
    {
        return table;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        N8nTableHandle that = (N8nTableHandle) o;
        return Objects.equals(schema, that.schema) && Objects.equals(table, that.table);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(schema, table);
    }

    @Override
    public String toString()
    {
        return schema + "." + table;
    }
}
