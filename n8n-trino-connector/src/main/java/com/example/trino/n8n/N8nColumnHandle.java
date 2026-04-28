package com.example.trino.n8n;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.trino.spi.connector.ColumnHandle;
import io.trino.spi.type.Type;

import java.util.Objects;

public class N8nColumnHandle
        implements ColumnHandle
{
    private final String name;
    private final Type type;

    @JsonCreator
    public N8nColumnHandle(
            @JsonProperty("name") String name,
            @JsonProperty("type") Type type)
    {
        this.name = name;
        this.type = type;
    }

    @JsonProperty
    public String getName()
    {
        return name;
    }

    @JsonProperty
    public Type getType()
    {
        return type;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        N8nColumnHandle that = (N8nColumnHandle) o;
        return Objects.equals(name, that.name) && Objects.equals(type, that.type);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(name, type);
    }

    @Override
    public String toString()
    {
        return name + ":" + type;
    }
}
