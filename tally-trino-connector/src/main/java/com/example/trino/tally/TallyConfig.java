package com.example.trino.tally;

import java.util.Objects;

import static java.util.Objects.requireNonNull;

public class TallyConfig
{
    private final String endpoint;
    private final String company;

    public TallyConfig(String endpoint, String company)
    {
        this.endpoint = requireNonNull(endpoint, "endpoint is null");
        this.company = requireNonNull(company, "company is null");
    }

    public String getEndpoint()
    {
        return endpoint;
    }

    public String getCompany()
    {
        return company;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) {
            return true;
        }
        if (!(o instanceof TallyConfig that)) {
            return false;
        }
        return Objects.equals(endpoint, that.endpoint) && Objects.equals(company, that.company);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(endpoint, company);
    }

    @Override
    public String toString()
    {
        return "TallyConfig{endpoint='" + endpoint + "', company='" + company + "'}";
    }
}
