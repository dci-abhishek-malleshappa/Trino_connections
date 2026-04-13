package com.zoho.trino;

import io.trino.spi.connector.ColumnHandle;
import io.trino.spi.connector.ConnectorRecordSetProvider;
import io.trino.spi.connector.ConnectorSession;
import io.trino.spi.connector.ConnectorSplit;
import io.trino.spi.connector.ConnectorTableHandle;
import io.trino.spi.connector.ConnectorTransactionHandle;
import io.trino.spi.connector.RecordCursor;
import io.trino.spi.connector.RecordSet;
import io.trino.spi.type.Type;

import java.util.List;

public final class ZohoRecordSetProvider
        implements ConnectorRecordSetProvider
{
    private final ZohoApiClient apiClient;

    public ZohoRecordSetProvider(ZohoApiClient apiClient)
    {
        this.apiClient = apiClient;
    }

    @Override
    public RecordSet getRecordSet(
            ConnectorTransactionHandle transaction,
            ConnectorSession session,
            ConnectorSplit split,
            ConnectorTableHandle table,
            List<? extends ColumnHandle> columns)
    {
        ZohoTableHandle tableHandle = (ZohoTableHandle) table;
        List<ZohoColumnHandle> requestedColumns = columns.stream()
                .map(ZohoColumnHandle.class::cast)
                .toList();

        List<Type> columnTypes = requestedColumns.stream()
                .map(ZohoColumnHandle::type)
                .toList();

        List<ZohoRow> rows = apiClient.fetchModuleRows(tableHandle.moduleName());
        return new RecordSet()
        {
            @Override
            public List<Type> getColumnTypes()
            {
                return columnTypes;
            }

            @Override
            public RecordCursor cursor()
            {
                return new ZohoCursor(requestedColumns, rows);
            }
        };
    }
}
