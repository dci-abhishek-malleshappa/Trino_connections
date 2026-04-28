# Zoho Trino Connector

This module exposes Zoho Books modules to Trino through your existing Express backend.

## Expected flow

1. Trino loads the plugin from `/usr/lib/trino/plugin/zoho/`.
2. The catalog file enables it with `connector.name=zoho`.
3. The connector calls the Express API at `http://localhost:5001/api/zoho/<module>` by default.
   - If Trino runs in Docker and the Express server runs on the host, use `http://host.docker.internal:5001/api/zoho` instead.
4. The Express API refreshes the Zoho OAuth token and returns normalized JSON with a `data` array.
5. Trino infers table columns from the JSON keys and exposes each configured module as a table.

## Catalog example

```properties
connector.name=zoho
zoho.base-url=http://host.docker.internal:5001/api/zoho
zoho.schema-name=zoho
zoho.tables=contacts,items,invoices
zoho.organization-id=your_zoho_books_org_id
zoho.http-timeout-seconds=30
```

With `zoho.schema-name=zoho`, tables such as `zoho.contacts`, `zoho.items`, and `zoho.invoices` are available inside the selected catalog.

If your catalog is also named `zoho`, the fully qualified table name is `zoho.zoho.contacts`.

For multiple Zoho Books organizations, create multiple Trino catalogs with different `zoho.organization-id` values.
