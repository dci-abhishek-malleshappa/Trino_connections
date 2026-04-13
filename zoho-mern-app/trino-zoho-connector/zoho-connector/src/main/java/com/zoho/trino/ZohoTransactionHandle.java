package com.zoho.trino;

import io.trino.spi.connector.ConnectorTransactionHandle;

public enum ZohoTransactionHandle
        implements ConnectorTransactionHandle
{
    INSTANCE
}
