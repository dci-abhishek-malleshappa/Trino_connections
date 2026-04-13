package com.zoho.trino;

import io.airlift.slice.Slice;
import io.airlift.slice.Slices;
import io.trino.spi.connector.RecordCursor;
import io.trino.spi.type.Type;

import java.util.List;

public final class ZohoCursor
        implements RecordCursor
{
    private final List<ZohoColumnHandle> columns;
    private final List<ZohoRow> rows;
    private int rowIndex = -1;

    public ZohoCursor(List<ZohoColumnHandle> columns, List<ZohoRow> rows)
    {
        this.columns = columns;
        this.rows = rows;
    }

    @Override
    public long getCompletedBytes()
    {
        return 0;
    }

    @Override
    public long getReadTimeNanos()
    {
        return 0;
    }

    @Override
    public Type getType(int field)
    {
        return columns.get(field).type();
    }

    @Override
    public boolean advanceNextPosition()
    {
        rowIndex++;
        return rowIndex < rows.size();
    }

    @Override
    public boolean getBoolean(int field)
    {
        throw new UnsupportedOperationException("Zoho connector only exposes VARCHAR columns");
    }

    @Override
    public long getLong(int field)
    {
        throw new UnsupportedOperationException("Zoho connector only exposes VARCHAR columns");
    }

    @Override
    public double getDouble(int field)
    {
        throw new UnsupportedOperationException("Zoho connector only exposes VARCHAR columns");
    }

    @Override
    public Slice getSlice(int field)
    {
        String value = currentValue(field);
        return value == null ? null : Slices.utf8Slice(value);
    }

    @Override
    public Object getObject(int field)
    {
        throw new UnsupportedOperationException("Zoho connector only exposes VARCHAR columns");
    }

    @Override
    public boolean isNull(int field)
    {
        return currentValue(field) == null;
    }

    @Override
    public void close()
    {
    }

    private String currentValue(int field)
    {
        ZohoRow row = rows.get(rowIndex);
        String columnName = columns.get(field).name();
        if ("raw_json".equals(columnName)) {
            return row.rawJson();
        }
        return row.values().get(columnName);
    }
}
