package com.example.trino.n8n;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.trino.spi.HostAddress;
import io.trino.spi.connector.ConnectorSplit;

import java.util.List;

public class N8nSplit
        implements ConnectorSplit
{
    private final String schema;
    private final String table;

    @JsonCreator
    public N8nSplit(
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
    public boolean isRemotelyAccessible()
    {
        return true;
    }

    @Override
    public List<HostAddress> getAddresses()
    {
        return List.of();
    }

}
