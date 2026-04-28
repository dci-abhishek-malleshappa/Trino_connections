package com.example.trino.n8n;

import io.trino.spi.connector.Connector;
import io.trino.spi.connector.ConnectorContext;
import io.trino.spi.connector.ConnectorFactory;

import java.util.Map;

public class N8nConnectorFactory
        implements ConnectorFactory
{
    private final String name;

    public N8nConnectorFactory()
    {
        this("n8n");
    }

    public N8nConnectorFactory(String name)
    {
        this.name = name;
    }

    @Override
    public String getName()
    {
        return name;
    }

    @Override
    public Connector create(String catalogName, Map<String, String> config, ConnectorContext context)
    {
        N8nConfig n8nConfig = new N8nConfig(config);
        return new N8nConnector(n8nConfig);
    }
}
