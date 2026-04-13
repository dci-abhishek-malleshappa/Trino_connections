package com.example.trino.tally;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.trino.spi.connector.ConnectorTableHandle;

import java.util.Objects;

public class TallyTableHandle implements ConnectorTableHandle
{
    private final String schemaName;
    private final String tableName;

    @JsonCreator
    public TallyTableHandle(
            @JsonProperty("schemaName") String schemaName,
            @JsonProperty("tableName") String tableName)
    {
        this.schemaName = schemaName;
        this.tableName = tableName;
    }

    @JsonProperty
    public String getSchemaName()
    {
        return schemaName;
    }

    @JsonProperty
    public String getTableName()
    {
        return tableName;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) {
            return true;
        }
        if (!(o instanceof TallyTableHandle that)) {
            return false;
        }
        return Objects.equals(schemaName, that.schemaName) && Objects.equals(tableName, that.tableName);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(schemaName, tableName);
    }

    @Override
    public String toString()
    {
        return schemaName + "." + tableName;
    }
}
