package com.example.trino.tally;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public class TallyRow
{
    private final Map<String, Object> values;

    public TallyRow(Map<String, Object> values)
    {
        this.values = Collections.unmodifiableMap(new LinkedHashMap<>(values));
    }

    public Object getValue(String columnName)
    {
        return values.get(columnName);
    }

    public Map<String, Object> getValues()
    {
        return values;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) {
            return true;
        }
        if (!(o instanceof TallyRow tallyRow)) {
            return false;
        }
        return Objects.equals(values, tallyRow.values);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(values);
    }

    @Override
    public String toString()
    {
        return "TallyRow{" + "values=" + values + '}';
    }
}
