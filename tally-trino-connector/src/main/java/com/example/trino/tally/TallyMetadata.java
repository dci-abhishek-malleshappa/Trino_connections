package com.example.trino.tally;

import io.trino.spi.connector.ColumnHandle;
import io.trino.spi.connector.ColumnMetadata;
import io.trino.spi.connector.ConnectorMetadata;
import io.trino.spi.connector.ConnectorSession;
import io.trino.spi.connector.ConnectorTableHandle;
import io.trino.spi.connector.ConnectorTableMetadata;
import io.trino.spi.connector.ConnectorTableVersion;
import io.trino.spi.connector.SchemaTableName;
import io.trino.spi.connector.SchemaTablePrefix;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class TallyMetadata implements ConnectorMetadata
{
    @Override
    public List<String> listSchemaNames(ConnectorSession session)
    {
        return List.of(TallyTableRegistry.SCHEMA_NAME);
    }

    @Override
    public ConnectorTableHandle getTableHandle(
            ConnectorSession session,
            SchemaTableName tableName,
            Optional<ConnectorTableVersion> startVersion,
            Optional<ConnectorTableVersion> endVersion)
    {
        return TallyTableRegistry.getTable(tableName)
                .map(table -> new TallyTableHandle(tableName.getSchemaName(), tableName.getTableName()))
                .orElse(null);
    }

    @Override
    public ConnectorTableMetadata getTableMetadata(ConnectorSession session, ConnectorTableHandle table)
    {
        TallyTableHandle tableHandle = (TallyTableHandle) table;
        TallyTableDefinition tableDefinition = getRequiredTableDefinition(tableHandle);
        return new ConnectorTableMetadata(
                new SchemaTableName(tableHandle.getSchemaName(), tableHandle.getTableName()),
                tableDefinition.getColumnMetadata());
    }

    @Override
    public List<SchemaTableName> listTables(ConnectorSession session, Optional<String> schemaName)
    {
        if (schemaName.isPresent() && !TallyTableRegistry.SCHEMA_NAME.equals(schemaName.get())) {
            return List.of();
        }
        return TallyTableRegistry.listTables().stream()
                .map(TallyTableDefinition::getSchemaTableName)
                .toList();
    }

    @Override
    public Map<String, ColumnHandle> getColumnHandles(ConnectorSession session, ConnectorTableHandle tableHandle)
    {
        TallyTableHandle handle = (TallyTableHandle) tableHandle;
        return new LinkedHashMap<>(getRequiredTableDefinition(handle).getColumnHandles());
    }

    @Override
    public ColumnMetadata getColumnMetadata(ConnectorSession session, ConnectorTableHandle tableHandle, ColumnHandle columnHandle)
    {
        return ((TallyColumnHandle) columnHandle).getColumnMetadata();
    }

    @Override
    public Map<SchemaTableName, List<ColumnMetadata>> listTableColumns(ConnectorSession session, SchemaTablePrefix prefix)
    {
        Map<SchemaTableName, List<ColumnMetadata>> tables = new LinkedHashMap<>();
        for (TallyTableDefinition tableDefinition : TallyTableRegistry.listTables()) {
            if (prefix.matches(tableDefinition.getSchemaTableName())) {
                tables.put(tableDefinition.getSchemaTableName(), tableDefinition.getColumnMetadata());
            }
        }
        return tables;
    }

    private static TallyTableDefinition getRequiredTableDefinition(TallyTableHandle tableHandle)
    {
        return TallyTableRegistry.getTable(tableHandle.getSchemaName(), tableHandle.getTableName())
                .orElseThrow(() -> new IllegalArgumentException("Unsupported table: " + tableHandle));
    }
}
