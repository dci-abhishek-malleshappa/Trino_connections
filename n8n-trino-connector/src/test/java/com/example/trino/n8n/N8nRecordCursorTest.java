package com.example.trino.n8n;

import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;

import static io.trino.spi.type.BigintType.BIGINT;
import static io.trino.spi.type.TimestampType.createTimestampType;
import static io.trino.spi.type.VarcharType.VARCHAR;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class N8nRecordCursorTest
{
    @Test
    void fetchesRowsAcrossMultiplePages()
    {
        N8nClient client = new StubN8nClient(List.of(
                new N8nClient.N8nDataPage(List.of(Map.of("id", 1L), Map.of("id", 2L)), OptionalInt.of(2)),
                new N8nClient.N8nDataPage(List.of(Map.of("id", 3L)), OptionalInt.empty())));

        N8nRecordCursor cursor = new N8nRecordCursor(
                client,
                new N8nSplit("sales", "orders"),
                List.of(new N8nColumnHandle("id", BIGINT)));

        assertTrue(cursor.advanceNextPosition());
        assertEquals(1L, cursor.getLong(0));
        assertTrue(cursor.advanceNextPosition());
        assertEquals(2L, cursor.getLong(0));
        assertTrue(cursor.advanceNextPosition());
        assertEquals(3L, cursor.getLong(0));
        assertFalse(cursor.advanceNextPosition());
    }

    @Test
    void parsesOffsetTimestampsToTrinoMicros()
    {
        N8nClient client = new StubN8nClient(List.of(
                new N8nClient.N8nDataPage(
                        List.of(Map.of("created_at", "2026-04-23T10:15:30Z", "name", "alpha")),
                        OptionalInt.empty())));

        N8nRecordCursor cursor = new N8nRecordCursor(
                client,
                new N8nSplit("sales", "orders"),
                List.of(
                        new N8nColumnHandle("created_at", createTimestampType(3)),
                        new N8nColumnHandle("name", VARCHAR)));

        assertTrue(cursor.advanceNextPosition());
        assertEquals(OffsetDateTime.parse("2026-04-23T10:15:30Z").toInstant().toEpochMilli() * 1_000, cursor.getLong(0));
        assertEquals("alpha", cursor.getSlice(1).toStringUtf8());
    }

    private static final class StubN8nClient
            extends N8nClient
    {
        private final List<N8nDataPage> pages;
        private int index;

        private StubN8nClient(List<N8nDataPage> pages)
        {
            super(new N8nConfig(Map.of("n8n.base-url", "http://localhost")));
            this.pages = pages;
        }

        @Override
        public N8nDataPage getDataPage(String schema, String table, int offset)
        {
            return pages.get(index++);
        }
    }
}
