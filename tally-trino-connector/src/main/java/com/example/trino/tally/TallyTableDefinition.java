package com.example.trino.tally;

import io.trino.spi.connector.ColumnHandle;
import io.trino.spi.connector.ColumnMetadata;
import io.trino.spi.connector.SchemaTableName;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class TallyTableDefinition
{
    private final String schemaName;
    private final String tableName;
    private final String tallyType;
    private final String rowTagName;
    private final List<TallyColumnDefinition> columns;

    public TallyTableDefinition(
            String schemaName,
            String tableName,
            String tallyType,
            String rowTagName,
            List<TallyColumnDefinition> columns)
    {
        this.schemaName = schemaName;
        this.tableName = tableName;
        this.tallyType = tallyType;
        this.rowTagName = rowTagName;
        this.columns = List.copyOf(columns);
    }

    public String getSchemaName()
    {
        return schemaName;
    }

    public String getTableName()
    {
        return tableName;
    }

    public String getTallyType()
    {
        return tallyType;
    }

    public String getRowTagName()
    {
        return rowTagName;
    }

    public SchemaTableName getSchemaTableName()
    {
        return new SchemaTableName(schemaName, tableName);
    }

    public List<TallyColumnDefinition> getColumns()
    {
        return columns;
    }

    public List<ColumnMetadata> getColumnMetadata()
    {
        return columns.stream()
                .map(TallyColumnDefinition::toColumnMetadata)
                .toList();
    }

    public Map<String, ColumnHandle> getColumnHandles()
    {
        Map<String, ColumnHandle> handles = new LinkedHashMap<>();
        for (int i = 0; i < columns.size(); i++) {
            TallyColumnDefinition column = columns.get(i);
            handles.put(column.columnName(), new TallyColumnHandle(column.columnName(), column.typeName(), i));
        }
        return handles;
    }
}
