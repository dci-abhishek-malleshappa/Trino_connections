package com.example.trino.tally;

import io.airlift.slice.Slice;
import io.airlift.slice.Slices;
import io.trino.spi.connector.RecordCursor;
import io.trino.spi.type.Type;

import java.util.List;

import static java.util.Objects.requireNonNull;

public class TallyRecordCursor implements RecordCursor
{
    private final List<TallyRow> rows;
    private final List<TallyColumnHandle> columns;
    private int position = -1;

    public TallyRecordCursor(List<TallyRow> rows, List<TallyColumnHandle> columns)
    {
        this.rows = List.copyOf(requireNonNull(rows, "rows is null"));
        this.columns = List.copyOf(requireNonNull(columns, "columns is null"));
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
        return columns.get(field).getType();
    }

    @Override
    public boolean advanceNextPosition()
    {
        position++;
        return position < rows.size();
    }

    @Override
    public boolean getBoolean(int field)
    {
        throw new UnsupportedOperationException("Boolean fields are not supported");
    }

    @Override
    public long getLong(int field)
    {
        throw new UnsupportedOperationException("Long fields are not supported");
    }

    @Override
    public double getDouble(int field)
    {
        Object value = getValue(field);
        if (value == null) {
            return 0;
        }
        return (double) value;
    }

    @Override
    public Slice getSlice(int field)
    {
        Object value = getValue(field);
        if (value == null) {
            return Slices.utf8Slice("");
        }
        return Slices.utf8Slice((String) value);
    }

    @Override
    public Object getObject(int field)
    {
        return getValue(field);
    }

    @Override
    public boolean isNull(int field)
    {
        return getValue(field) == null;
    }

    @Override
    public void close()
    {
    }

    private Object getValue(int field)
    {
        TallyRow row = rows.get(position);
        return row.getValue(columns.get(field).getColumnName());
    }
}
