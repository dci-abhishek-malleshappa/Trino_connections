package com.example.trino.n8n;

import io.trino.spi.connector.RecordCursor;
import io.trino.spi.connector.RecordSet;
import io.trino.spi.type.Type;

import java.util.List;

public class N8nRecordSet
        implements RecordSet
{
    private final N8nClient client;
    private final N8nSplit split;
    private final List<N8nColumnHandle> columns;

    public N8nRecordSet(N8nClient client, N8nSplit split, List<N8nColumnHandle> columns)
    {
        this.client = client;
        this.split = split;
        this.columns = columns;
    }

    @Override
    public List<Type> getColumnTypes()
    {
        return columns.stream()
                .map(N8nColumnHandle::getType)
                .toList();
    }

    @Override
    public RecordCursor cursor()
    {
        return new N8nRecordCursor(client, split, columns);
    }
}