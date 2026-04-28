package com.example.trino.n8n;

import io.trino.spi.connector.Constraint;
import io.trino.spi.connector.ConnectorSession;
import io.trino.spi.connector.ConnectorSplit;
import io.trino.spi.connector.ConnectorSplitManager;
import io.trino.spi.connector.ConnectorSplitSource;
import io.trino.spi.connector.ConnectorTableHandle;
import io.trino.spi.connector.ConnectorTransactionHandle;
import io.trino.spi.connector.DynamicFilter;
import io.trino.spi.connector.FixedSplitSource;

import java.util.List;

public class N8nSplitManager
        implements ConnectorSplitManager
{
    @Override
    public ConnectorSplitSource getSplits(
            ConnectorTransactionHandle transaction,
            ConnectorSession session,
            ConnectorTableHandle tableHandle,
            DynamicFilter dynamicFilter,
            Constraint constraint)
    {
        N8nTableHandle handle = (N8nTableHandle) tableHandle;
        ConnectorSplit split = new N8nSplit(handle.getSchema(), handle.getTable());
        return new FixedSplitSource(List.of(split));
    }
}
