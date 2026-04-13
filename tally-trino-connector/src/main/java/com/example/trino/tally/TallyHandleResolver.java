package com.example.trino.tally;

// Trino 431 resolves connector handles without a ConnectorHandleResolver implementation.
// This placeholder keeps the project structure requested by the user and gives us a clear
// extension point if an older SPI version needs explicit handle resolution wiring.
public final class TallyHandleResolver
{
    private TallyHandleResolver()
    {
    }
}
