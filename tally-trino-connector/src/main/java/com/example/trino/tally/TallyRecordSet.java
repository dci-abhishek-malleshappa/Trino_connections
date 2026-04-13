package com.example.trino.tally;

import io.trino.spi.connector.RecordCursor;
import io.trino.spi.connector.RecordSet;
import io.trino.spi.type.Type;

import java.util.List;

import static java.util.Objects.requireNonNull;

public class TallyRecordSet implements RecordSet
{
    private final TallyClient tallyClient;
    private final TallyTableHandle table;
    private final List<TallyColumnHandle> columns;

    public TallyRecordSet(TallyClient tallyClient, TallyTableHandle table, List<TallyColumnHandle> columns)
    {
        this.tallyClient = requireNonNull(tallyClient, "tallyClient is null");
        this.table = requireNonNull(table, "table is null");
        this.columns = List.copyOf(requireNonNull(columns, "columns is null"));
    }

    @Override
    public List<Type> getColumnTypes()
    {
        return columns.stream()
                .map(TallyColumnHandle::getType)
                .toList();
    }

    @Override
    public RecordCursor cursor()
    {
        TallyTableDefinition tableDefinition = TallyTableRegistry.getTable(table.getSchemaName(), table.getTableName())
                .orElseThrow(() -> new IllegalArgumentException("Unsupported table: " + table));
        return new TallyRecordCursor(tallyClient.getRows(tableDefinition), columns);
    }
}
