package com.zoho.trino;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.trino.spi.connector.ColumnHandle;
import io.trino.spi.type.Type;

import java.util.Objects;

public final class ZohoColumnHandle
        implements ColumnHandle
{
    private final String name;
    private final Type type;

    @JsonCreator
    public ZohoColumnHandle(
            @JsonProperty("name") String name,
            @JsonProperty("type") Type type)
    {
        this.name = Objects.requireNonNull(name, "name is null");
        this.type = Objects.requireNonNull(type, "type is null");
    }

    @JsonProperty
    public String name()
    {
        return name;
    }

    @JsonProperty
    public Type type()
    {
        return type;
    }

    @Override
    public boolean equals(Object other)
    {
        if (this == other) {
            return true;
        }
        if (!(other instanceof ZohoColumnHandle that)) {
            return false;
        }
        return name.equals(that.name) && type.equals(that.type);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(name, type);
    }
}
