package com.example.trino.n8n;

import io.trino.spi.connector.RecordCursor;
import io.trino.spi.type.Type;
import io.airlift.slice.Slice;
import io.airlift.slice.Slices;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;

public class N8nRecordCursor
        implements RecordCursor
{
    private final N8nClient client;
    private final N8nSplit split;
    private final List<N8nColumnHandle> columns;
    private Iterator<Map<String, Object>> dataIterator = Collections.emptyIterator();
    private Map<String, Object> currentRow;
    private OptionalInt nextOffset = OptionalInt.of(0);
    private boolean finished;

    public N8nRecordCursor(N8nClient client, N8nSplit split, List<N8nColumnHandle> columns)
    {
        this.client = client;
        this.split = split;
        this.columns = columns;
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
        while (true) {
            if (dataIterator.hasNext()) {
                currentRow = dataIterator.next();
                return true;
            }
            if (finished) {
                currentRow = null;
                return false;
            }
            fetchNextPage();
        }
    }

    @Override
    public boolean getBoolean(int field)
    {
        return (Boolean) getValue(field);
    }

    @Override
    public long getLong(int field)
    {
        Number number = (Number) getValue(field);
        return number.longValue();
    }

    @Override
    public double getDouble(int field)
    {
        Number number = (Number) getValue(field);
        return number.doubleValue();
    }

    @Override
    public Slice getSlice(int field)
    {
        Object value = getValue(field);
        return value == null ? null : Slices.utf8Slice(value.toString());
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
        // No resources to close
    }

    private Object getValue(int field)
    {
        if (currentRow == null) {
            throw new IllegalStateException("Cursor is not positioned on a row");
        }
        String columnName = columns.get(field).getName();
        Object value = currentRow.get(columnName);
        if (value == null) {
            return null;
        }
        Type type = columns.get(field).getType();
        if (type.equals(io.trino.spi.type.DateType.DATE)) {
            return LocalDate.parse(value.toString(), DateTimeFormatter.ISO_LOCAL_DATE).toEpochDay();
        }
        if (type.equals(io.trino.spi.type.TimestampType.createTimestampType(3))) {
            return parseTimestampMicros(value.toString());
        }
        return value;
    }

    private long parseTimestampMicros(String value)
    {
        try {
            return OffsetDateTime.parse(value, DateTimeFormatter.ISO_DATE_TIME)
                    .toInstant()
                    .toEpochMilli() * 1_000;
        }
        catch (DateTimeParseException ignored) {
            return LocalDateTime.parse(value, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                    .toInstant(ZoneOffset.UTC)
                    .toEpochMilli() * 1_000;
        }
    }

    private void fetchNextPage()
    {
        if (nextOffset.isEmpty()) {
            finished = true;
            dataIterator = Collections.emptyIterator();
            return;
        }

        N8nClient.N8nDataPage page = client.getDataPage(split.getSchema(), split.getTable(), nextOffset.getAsInt());
        dataIterator = page.data().iterator();
        nextOffset = page.nextOffset();
        finished = !dataIterator.hasNext() && nextOffset.isEmpty();
    }
}
