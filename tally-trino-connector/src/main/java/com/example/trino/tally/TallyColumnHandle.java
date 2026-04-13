package com.example.trino.tally;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.trino.spi.connector.ColumnHandle;
import io.trino.spi.connector.ColumnMetadata;
import io.trino.spi.type.DoubleType;
import io.trino.spi.type.Type;
import io.trino.spi.type.VarcharType;

import java.util.Objects;

public class TallyColumnHandle implements ColumnHandle
{
    private final String columnName;
    private final String typeName;
    private final int ordinalPosition;

    @JsonCreator
    public TallyColumnHandle(
            @JsonProperty("columnName") String columnName,
            @JsonProperty("typeName") String typeName,
            @JsonProperty("ordinalPosition") int ordinalPosition)
    {
        this.columnName = columnName;
        this.typeName = typeName;
        this.ordinalPosition = ordinalPosition;
    }

    @JsonProperty
    public String getColumnName()
    {
        return columnName;
    }

    @JsonProperty
    public String getTypeName()
    {
        return typeName;
    }

    public Type getType()
    {
        return switch (typeName) {
            case "varchar" -> VarcharType.VARCHAR;
            case "double" -> DoubleType.DOUBLE;
            default -> throw new IllegalArgumentException("Unsupported type: " + typeName);
        };
    }

    @JsonProperty
    public int getOrdinalPosition()
    {
        return ordinalPosition;
    }

    public ColumnMetadata getColumnMetadata()
    {
        return new ColumnMetadata(columnName, getType());
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) {
            return true;
        }
        if (!(o instanceof TallyColumnHandle that)) {
            return false;
        }
        return ordinalPosition == that.ordinalPosition &&
                Objects.equals(columnName, that.columnName) &&
                Objects.equals(typeName, that.typeName);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(columnName, typeName, ordinalPosition);
    }

    @Override
    public String toString()
    {
        return columnName + ":" + typeName;
    }
}
