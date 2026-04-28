package com.example.trino.n8n;

import io.trino.spi.connector.ColumnHandle;
import io.trino.spi.connector.ConnectorRecordSetProvider;
import io.trino.spi.connector.ConnectorSession;
import io.trino.spi.connector.ConnectorSplit;
import io.trino.spi.connector.ConnectorTableHandle;
import io.trino.spi.connector.ConnectorTransactionHandle;
import io.trino.spi.connector.RecordSet;

import java.util.List;

public class N8nRecordSetProvider
        implements ConnectorRecordSetProvider
{
    private final N8nClient client;

    public N8nRecordSetProvider(N8nClient client)
    {
        this.client = client;
    }

    @Override
    public RecordSet getRecordSet(ConnectorTransactionHandle transaction, ConnectorSession session, ConnectorSplit split, ConnectorTableHandle tableHandle, List<? extends ColumnHandle> columns)
    {
        N8nSplit n8nSplit = (N8nSplit) split;
        List<N8nColumnHandle> n8nColumns = columns.stream()
                .map(N8nColumnHandle.class::cast)
                .toList();
        return new N8nRecordSet(client, n8nSplit, n8nColumns);
    }
}
