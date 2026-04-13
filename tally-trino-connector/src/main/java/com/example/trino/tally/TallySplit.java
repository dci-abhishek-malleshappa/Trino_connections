package com.example.trino.tally;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.trino.spi.HostAddress;
import io.trino.spi.connector.ConnectorSplit;

import java.util.List;
import java.util.Objects;

public class TallySplit implements ConnectorSplit
{
    private final int splitId;

    @JsonCreator
    public TallySplit(@JsonProperty("splitId") int splitId)
    {
        this.splitId = splitId;
    }

    @JsonProperty
    public int getSplitId()
    {
        return splitId;
    }

    @Override
    public boolean isRemotelyAccessible()
    {
        return true;
    }

    @Override
    public List<HostAddress> getAddresses()
    {
        return List.of();
    }

    @Override
    public Object getInfo()
    {
        return this;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) {
            return true;
        }
        if (!(o instanceof TallySplit that)) {
            return false;
        }
        return splitId == that.splitId;
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(splitId);
    }

    @Override
    public String toString()
    {
        return "TallySplit{splitId=" + splitId + '}';
    }
}
