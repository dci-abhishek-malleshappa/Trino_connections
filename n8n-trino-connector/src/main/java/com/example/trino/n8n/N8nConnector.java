package com.example.trino.n8n;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.trino.spi.connector.Connector;
import io.trino.spi.connector.ConnectorSession;
import io.trino.spi.connector.ConnectorMetadata;
import io.trino.spi.connector.ConnectorRecordSetProvider;
import io.trino.spi.connector.ConnectorSplitManager;
import io.trino.spi.connector.ConnectorTransactionHandle;
import io.trino.spi.transaction.IsolationLevel;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class N8nConnector
        implements Connector
{
    private final N8nConfig config;
    private final N8nClient client;
    private final ConcurrentMap<ConnectorTransactionHandle, N8nMetadata> transactions = new ConcurrentHashMap<>();

    public N8nConnector(N8nConfig config)
    {
        this.config = config;
        this.client = new N8nClient(config);
    }

    @Override
    public ConnectorTransactionHandle beginTransaction(IsolationLevel isolationLevel, boolean readOnly, boolean autoCommit)
    {
        ConnectorTransactionHandle handle = new N8nTransactionHandle(UUID.randomUUID().toString());
        transactions.put(handle, new N8nMetadata(client));
        return handle;
    }

    @Override
    public ConnectorMetadata getMetadata(ConnectorSession session, ConnectorTransactionHandle transactionHandle)
    {
        return transactions.get(transactionHandle);
    }

    @Override
    public ConnectorSplitManager getSplitManager()
    {
        return new N8nSplitManager();
    }

    @Override
    public ConnectorRecordSetProvider getRecordSetProvider()
    {
        return new N8nRecordSetProvider(client);
    }

    @Override
    public void commit(ConnectorTransactionHandle transactionHandle)
    {
        transactions.remove(transactionHandle);
    }

    @Override
    public void rollback(ConnectorTransactionHandle transactionHandle)
    {
        transactions.remove(transactionHandle);
    }

    @Override
    public void shutdown()
    {
        transactions.clear();
    }

    public static class N8nTransactionHandle
            implements ConnectorTransactionHandle
    {
        private final String id;

        @JsonCreator
        public N8nTransactionHandle(@JsonProperty("id") String id)
        {
            this.id = id;
        }

        @JsonProperty
        public String getId()
        {
            return id;
        }

        @Override
        public boolean equals(Object o)
        {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            N8nTransactionHandle that = (N8nTransactionHandle) o;
            return Objects.equals(id, that.id);
        }

        @Override
        public int hashCode()
        {
            return Objects.hash(id);
        }
    }
}
