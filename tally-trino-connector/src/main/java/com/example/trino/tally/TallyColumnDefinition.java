package com.example.trino.tally;

import io.trino.spi.connector.ColumnMetadata;
import io.trino.spi.type.DoubleType;
import io.trino.spi.type.VarcharType;

import java.util.List;

public record TallyColumnDefinition(String columnName, String typeName, String nativeMethod, List<String> sourceNames)
{
    public TallyColumnDefinition
    {
        sourceNames = List.copyOf(sourceNames);
    }

    public ColumnMetadata toColumnMetadata()
    {
        return new ColumnMetadata(columnName, switch (typeName) {
            case "varchar" -> VarcharType.VARCHAR;
            case "double" -> DoubleType.DOUBLE;
            default -> throw new IllegalArgumentException("Unsupported type: " + typeName);
        });
    }
}
