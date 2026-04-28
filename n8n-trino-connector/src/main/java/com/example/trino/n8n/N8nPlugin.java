package com.example.trino.n8n;

import io.trino.spi.Plugin;
import io.trino.spi.connector.ConnectorFactory;

import java.util.Set;

public class N8nPlugin
        implements Plugin
{
    @Override
    public Set<ConnectorFactory> getConnectorFactories()
    {
        return Set.of(
                new N8nConnectorFactory("n8n"),
                new N8nConnectorFactory("zoho_books"));
    }
}
