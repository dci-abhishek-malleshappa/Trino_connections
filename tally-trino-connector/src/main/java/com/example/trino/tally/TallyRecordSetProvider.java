package com.example.trino.tally;

import io.trino.spi.connector.ColumnHandle;
import io.trino.spi.connector.ConnectorRecordSetProvider;
import io.trino.spi.connector.ConnectorSession;
import io.trino.spi.connector.ConnectorSplit;
import io.trino.spi.connector.ConnectorTableHandle;
import io.trino.spi.connector.ConnectorTransactionHandle;
import io.trino.spi.connector.RecordSet;

import java.util.List;

import static java.util.Objects.requireNonNull;

public class TallyRecordSetProvider implements ConnectorRecordSetProvider
{
    private final TallyClient tallyClient;

    public TallyRecordSetProvider(TallyClient tallyClient)
    {
        this.tallyClient = requireNonNull(tallyClient, "tallyClient is null");
    }

    @Override
    public RecordSet getRecordSet(
            ConnectorTransactionHandle transaction,
            ConnectorSession session,
            ConnectorSplit split,
            ConnectorTableHandle table,
            List<? extends ColumnHandle> columns)
    {
        return new TallyRecordSet(
                tallyClient,
                (TallyTableHandle) table,
                columns.stream()
                        .map(TallyColumnHandle.class::cast)
                        .toList());
    }
}
