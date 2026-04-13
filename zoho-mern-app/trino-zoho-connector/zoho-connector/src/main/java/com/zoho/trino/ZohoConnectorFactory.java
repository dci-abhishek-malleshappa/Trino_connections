package com.zoho.trino;

import io.trino.spi.connector.Connector;
import io.trino.spi.connector.ConnectorContext;
import io.trino.spi.connector.ConnectorFactory;

import java.util.Map;
import java.util.Objects;

public final class ZohoConnectorFactory
        implements ConnectorFactory
{
    @Override
    public String getName()
    {
        return "zoho";
    }

    @Override
    public Connector create(String catalogName, Map<String, String> config, ConnectorContext context)
    {
        Objects.requireNonNull(catalogName, "catalogName is null");
        Objects.requireNonNull(config, "config is null");
        Objects.requireNonNull(context, "context is null");

        return new ZohoConnector(ZohoConfig.from(config));
    }
}
