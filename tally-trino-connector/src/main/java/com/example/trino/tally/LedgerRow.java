package com.example.trino.tally;

import java.util.Objects;

public class LedgerRow
{
    private final String ledgerName;
    private final String parentGroup;
    private final Double closingBalance;

    public LedgerRow(String ledgerName, String parentGroup, Double closingBalance)
    {
        this.ledgerName = ledgerName;
        this.parentGroup = parentGroup;
        this.closingBalance = closingBalance;
    }

    public String getLedgerName()
    {
        return ledgerName;
    }

    public String getParentGroup()
    {
        return parentGroup;
    }

    public Double getClosingBalance()
    {
        return closingBalance;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) {
            return true;
        }
        if (!(o instanceof LedgerRow that)) {
            return false;
        }
        return Objects.equals(ledgerName, that.ledgerName) &&
                Objects.equals(parentGroup, that.parentGroup) &&
                Objects.equals(closingBalance, that.closingBalance);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(ledgerName, parentGroup, closingBalance);
    }

    @Override
    public String toString()
    {
        return "LedgerRow{ledgerName='" + ledgerName + "', parentGroup='" + parentGroup + "', closingBalance=" + closingBalance + '}';
    }
}
