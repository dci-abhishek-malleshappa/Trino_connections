package com.example.trino.n8n;

import io.trino.spi.connector.ColumnHandle;
import io.trino.spi.connector.ColumnMetadata;
import io.trino.spi.connector.ConnectorMetadata;
import io.trino.spi.connector.ConnectorSession;
import io.trino.spi.connector.ConnectorTableHandle;
import io.trino.spi.connector.ConnectorTableMetadata;
import io.trino.spi.connector.ConnectorTableVersion;
import io.trino.spi.connector.SchemaTableName;
import io.trino.spi.type.Type;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class N8nMetadata
        implements ConnectorMetadata
{
    private final N8nClient client;

    public N8nMetadata(N8nClient client)
    {
        this.client = client;
    }

    @Override
    public List<String> listSchemaNames(ConnectorSession session)
    {
        return client.getSchemas();
    }

    @Override
    public ConnectorTableHandle getTableHandle(
            ConnectorSession session,
            SchemaTableName tableName,
            Optional<ConnectorTableVersion> startVersion,
            Optional<ConnectorTableVersion> endVersion)
    {
        if (startVersion.isPresent() || endVersion.isPresent()) {
            throw new UnsupportedOperationException("Table versioning is not supported");
        }
        try {
            client.getTableDefinition(tableName.getSchemaName(), tableName.getTableName());
            return new N8nTableHandle(tableName.getSchemaName(), tableName.getTableName());
        }
        catch (N8nClient.N8nHttpException e) {
            if (e.getStatusCode() == 404) {
                return null;
            }
            throw e;
        }
        catch (RuntimeException e) {
            throw e;
        }
        catch (Exception e) {
            return null;
        }
    }

    @Override
    public ConnectorTableMetadata getTableMetadata(ConnectorSession session, ConnectorTableHandle tableHandle)
    {
        N8nTableHandle handle = (N8nTableHandle) tableHandle;
        N8nClient.N8nTableDefinition def = client.getTableDefinition(handle.getSchema(), handle.getTable());
        List<ColumnMetadata> columns = def.getColumns().stream()
                .map(col -> new ColumnMetadata(col.getName(), mapType(col.getType())))
                .collect(Collectors.toList());
        return new ConnectorTableMetadata(new SchemaTableName(handle.getSchema(), handle.getTable()), columns);
    }

    @Override
    public List<SchemaTableName> listTables(ConnectorSession session, Optional<String> optionalSchema)
    {
        if (optionalSchema.isEmpty()) {
            return listSchemaNames(session).stream()
                    .flatMap(schema -> client.getTables(schema).stream()
                            .map(table -> new SchemaTableName(schema, table)))
                    .collect(Collectors.toList());
        }
        else {
            String schema = optionalSchema.get();
            return client.getTables(schema).stream()
                    .map(table -> new SchemaTableName(schema, table))
                    .collect(Collectors.toList());
        }
    }

    @Override
    public Map<String, ColumnHandle> getColumnHandles(ConnectorSession session, ConnectorTableHandle tableHandle)
    {
        N8nTableHandle handle = (N8nTableHandle) tableHandle;
        N8nClient.N8nTableDefinition def = client.getTableDefinition(handle.getSchema(), handle.getTable());
        return def.getColumns().stream()
                .collect(Collectors.toMap(
                        N8nClient.N8nColumn::getName,
                        col -> new N8nColumnHandle(col.getName(), mapType(col.getType()))));
    }

    @Override
    public ColumnMetadata getColumnMetadata(ConnectorSession session, ConnectorTableHandle tableHandle, ColumnHandle columnHandle)
    {
        N8nColumnHandle handle = (N8nColumnHandle) columnHandle;
        return new ColumnMetadata(handle.getName(), handle.getType());
    }

    private Type mapType(String type)
    {
        switch (type.toLowerCase()) {
            case "varchar":
                return io.trino.spi.type.VarcharType.VARCHAR;
            case "bigint":
                return io.trino.spi.type.BigintType.BIGINT;
            case "integer":
                return io.trino.spi.type.IntegerType.INTEGER;
            case "boolean":
                return io.trino.spi.type.BooleanType.BOOLEAN;
            case "double":
                return io.trino.spi.type.DoubleType.DOUBLE;
            case "date":
                return io.trino.spi.type.DateType.DATE;
            case "timestamp":
                return io.trino.spi.type.TimestampType.createTimestampType(3);
            default:
                return io.trino.spi.type.VarcharType.VARCHAR;
        }
    }
}
