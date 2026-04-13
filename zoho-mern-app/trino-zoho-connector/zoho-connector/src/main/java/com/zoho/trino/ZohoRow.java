package com.zoho.trino;

import java.util.Map;

public record ZohoRow(Map<String, String> values, String rawJson)
{
}
