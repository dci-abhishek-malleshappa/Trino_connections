package com.example.trino.tally;

import io.trino.spi.connector.Connector;
import io.trino.spi.connector.ConnectorMetadata;
import io.trino.spi.connector.ConnectorRecordSetProvider;
import io.trino.spi.connector.ConnectorSession;
import io.trino.spi.connector.ConnectorSplitManager;
import io.trino.spi.connector.ConnectorTransactionHandle;
import io.trino.spi.transaction.IsolationLevel;

import static java.util.Objects.requireNonNull;

public class TallyConnector implements Connector
{
    private final TallyMetadata metadata;
    private final TallySplitManager splitManager;
    private final TallyRecordSetProvider recordSetProvider;

    public TallyConnector(TallyClient tallyClient)
    {
        requireNonNull(tallyClient, "tallyClient is null");
        this.metadata = new TallyMetadata();
        this.splitManager = new TallySplitManager();
        this.recordSetProvider = new TallyRecordSetProvider(tallyClient);
    }

    @Override
    public ConnectorTransactionHandle beginTransaction(IsolationLevel isolationLevel, boolean readOnly, boolean autoCommit)
    {
        return TallyTransactionHandle.INSTANCE;
    }

    @Override
    public ConnectorMetadata getMetadata(ConnectorSession session, ConnectorTransactionHandle transaction)
    {
        return metadata;
    }

    @Override
    public ConnectorSplitManager getSplitManager()
    {
        return splitManager;
    }

    @Override
    public ConnectorRecordSetProvider getRecordSetProvider()
    {
        return recordSetProvider;
    }

    public enum TallyTransactionHandle implements ConnectorTransactionHandle
    {
        INSTANCE
    }
}
