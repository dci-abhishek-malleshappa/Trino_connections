package com.zoho.trino;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.trino.spi.connector.ConnectorSplit;

public final class ZohoSplit
        implements ConnectorSplit
{
    private final String moduleName;

    @JsonCreator
    public ZohoSplit(@JsonProperty("moduleName") String moduleName)
    {
        this.moduleName = moduleName;
    }

    @JsonProperty
    public String getModuleName()
    {
        return moduleName;
    }

    @Override
    public Object getInfo()
    {
        return moduleName;
    }
}
