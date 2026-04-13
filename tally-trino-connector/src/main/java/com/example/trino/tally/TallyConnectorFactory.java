package com.example.trino.tally;

import io.trino.spi.connector.Connector;
import io.trino.spi.connector.ConnectorContext;
import io.trino.spi.connector.ConnectorFactory;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Map;

public class TallyConnectorFactory implements ConnectorFactory
{
    public static final String NAME = "tally";

    @Override
    public String getName()
    {
        return NAME;
    }

    @Override
    public Connector create(String catalogName, Map<String, String> config, ConnectorContext context)
    {
        TallyConfig tallyConfig = new TallyConfig(
                config.getOrDefault("endpoint", "http://localhost:9000"),
                config.getOrDefault("company", ""));

        TallyXmlParser parser = new TallyXmlParser();
        TallyClient client = new TallyXmlClient(
                tallyConfig,
                HttpClient.newBuilder()
                        .connectTimeout(Duration.ofSeconds(10))
                        .build(),
                parser);

        return new TallyConnector(client);
    }
}
