package com.zoho.trino;

import io.trino.spi.StandardErrorCode;
import io.trino.spi.TrinoException;
import io.trino.spi.connector.ColumnHandle;
import io.trino.spi.connector.ColumnMetadata;
import io.trino.spi.connector.ConnectorMetadata;
import io.trino.spi.connector.ConnectorSession;
import io.trino.spi.connector.ConnectorTableHandle;
import io.trino.spi.connector.ConnectorTableMetadata;
import io.trino.spi.connector.ConnectorTableVersion;
import io.trino.spi.connector.SchemaTableName;
import io.trino.spi.connector.SchemaTablePrefix;
import io.trino.spi.type.VarcharType;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class ZohoMetadata
        implements ConnectorMetadata
{
    private final ZohoConfig config;
    private final ZohoApiClient apiClient;

    public ZohoMetadata(ZohoConfig config, ZohoApiClient apiClient)
    {
        this.config = config;
        this.apiClient = apiClient;
    }

    @Override
    public List<String> listSchemaNames(ConnectorSession session)
    {
        return List.of(config.getSchemaName());
    }

    @Override
    public ConnectorTableHandle getTableHandle(ConnectorSession session, SchemaTableName tableName)
    {
        return buildTableHandle(tableName);
    }

    @Override
    public ConnectorTableHandle getTableHandle(
            ConnectorSession session,
            SchemaTableName tableName,
            Optional<ConnectorTableVersion> startVersion,
            Optional<ConnectorTableVersion> endVersion)
    {
        return buildTableHandle(tableName);
    }

    @Override
    public ConnectorTableMetadata getTableMetadata(ConnectorSession session, ConnectorTableHandle table)
    {
        ZohoTableHandle tableHandle = (ZohoTableHandle) table;
        return new ConnectorTableMetadata(tableHandle.schemaTableName(), inferColumns(tableHandle.moduleName()));
    }

    @Override
    public List<SchemaTableName> listTables(ConnectorSession session, Optional<String> schemaName)
    {
        if (schemaName.isPresent() && !config.getSchemaName().equals(schemaName.get())) {
            return List.of();
        }

        return config.getTableNames().stream()
                .map(table -> new SchemaTableName(config.getSchemaName(), table))
                .toList();
    }

    @Override
    public Map<String, ColumnHandle> getColumnHandles(ConnectorSession session, ConnectorTableHandle table)
    {
        ZohoTableHandle tableHandle = (ZohoTableHandle) table;
        List<ColumnMetadata> columns = inferColumns(tableHandle.moduleName());

        Map<String, ColumnHandle> handles = new LinkedHashMap<>();
        for (ColumnMetadata column : columns) {
            handles.put(column.getName(), new ZohoColumnHandle(column.getName(), column.getType()));
        }
        return handles;
    }

    @Override
    public ColumnMetadata getColumnMetadata(ConnectorSession session, ConnectorTableHandle tableHandle, ColumnHandle columnHandle)
    {
        ZohoColumnHandle handle = (ZohoColumnHandle) columnHandle;
        return new ColumnMetadata(handle.name(), handle.type());
    }

    @Override
    public Map<SchemaTableName, List<ColumnMetadata>> listTableColumns(ConnectorSession session, SchemaTablePrefix prefix)
    {
        Map<SchemaTableName, List<ColumnMetadata>> tableColumns = new LinkedHashMap<>();
        for (SchemaTableName tableName : listTables(session, prefix.getSchema())) {
            if (prefix.matches(tableName)) {
                tableColumns.put(tableName, inferColumns(tableName.getTableName()));
            }
        }
        return tableColumns;
    }

    private ConnectorTableHandle buildTableHandle(SchemaTableName tableName)
    {
        if (!config.getSchemaName().equals(tableName.getSchemaName())) {
            return null;
        }

        if (!config.getTableNames().contains(tableName.getTableName())) {
            return null;
        }

        return new ZohoTableHandle(tableName, tableName.getTableName());
    }

    private List<ColumnMetadata> inferColumns(String moduleName)
    {
        List<ZohoRow> rows = apiClient.fetchModuleRows(moduleName);
        LinkedHashMap<String, ColumnMetadata> columns = new LinkedHashMap<>();

        for (ZohoRow row : rows) {
            row.values().keySet().forEach(key -> columns.putIfAbsent(key, new ColumnMetadata(key, VarcharType.VARCHAR)));
        }

        columns.putIfAbsent("raw_json", new ColumnMetadata("raw_json", VarcharType.VARCHAR));

        if (columns.isEmpty()) {
            throw new TrinoException(StandardErrorCode.TABLE_NOT_FOUND, "No columns inferred for module: " + moduleName);
        }

        return List.copyOf(columns.values());
    }
}
