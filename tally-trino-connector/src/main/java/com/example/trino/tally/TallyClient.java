package com.example.trino.tally;

import java.util.List;

public interface TallyClient
{
    List<TallyRow> getRows(TallyTableDefinition tableDefinition);
}
