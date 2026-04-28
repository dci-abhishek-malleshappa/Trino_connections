# N8n Trino Connector

A custom Trino connector for reading data from n8n webhook/API endpoints.

---

## 1. Introduction

### 1.1 Purpose

The **N8n Trino Connector** is a custom plugin for [Trino](https://trino.io/) (formerly PrestoSQL) that enables SQL-based querying of data exposed through n8n workflow webhooks. This connector bridges the gap between Trino's powerful SQL engine and n8n's workflow automation platform, allowing data analysts and engineers to query business data from various integrations (Zoho, Shopify, Tally, etc.) using familiar SQL syntax.

### 1.2 Problem Statement

Organizations often use n8n to automate workflows and expose data via webhooks. However, querying this data requires:
- Building custom APIs
- Writing integration code
- Managing multiple data access patterns

This connector eliminates these challenges by treating n8n webhook endpoints as native Trino tables.

### 1.3 Use Cases

| Use Case | Description |
|----------|-------------|
| **Data Federation** | Combine n8n data with other data sources in Trino |
| **Ad-hoc Analysis** | Query n8n data using SQL for reporting |
| **ETL Pipelines** | Extract data from n8n workflows into data warehouses |
| **Dashboarding** | Connect BI tools to n8n data through Trino |

### 1.4 Version Information

| Component | Version |
|-----------|---------|
| Connector | 1.0.0 |
| Trino Server | 479 |
| Java | 17+ |
| Maven | 3.8+ |

---

## 2. Background

### 2.1 What is Trino?

Trino is a distributed SQL query engine designed for big data analytics. It provides:
- **Federated Queries**: Query multiple data sources simultaneously
- **Columnar Storage**: Optimized for analytical workloads
- **Parallel Execution**: Scale-out architecture for large datasets
- **Standard SQL**: ANSI-compliant SQL interface

### 2.2 What are Trino Connectors?

Trino connectors are plugins that allow Trino to communicate with external data sources. They implement the **Trino SPI (Service Provider Interface)** to provide:

- **Metadata**: Schema, table, and column definitions
- **Data Access**: Record reading and iteration
- **Split Management**: Parallel data partitioning

### 2.3 Why Build a Custom Connector?

Custom connectors are needed when:
- The data source doesn't have a native Trino connector
- Data is exposed via REST APIs (like n8n webhooks)
- You need tight integration with specific business logic

---

## 3. Architecture

## Project Structure

```
n8n-trino-connector/
├── pom.xml                          # Maven build configuration
├── n8n.properties                   # Trino catalog configuration
├── docker-n8n.properties            # Docker-based Trino config
├── README.md                        # This documentation
├── mock-n8n-service/               # Local mock service for testing
│   ├── server.py                    # Python HTTP mock server
│   ├── sample-data.json             # Sample data definitions
│   └── start-mock-n8n.ps1          # Windows startup script
└── src/
    ├── main/
    │   └── java/com/example/trino/n8n/
    │       ├── N8nClient.java          # HTTP client for n8n API
    │       ├── N8nConfig.java           # Connector configuration
    │       ├── N8nConnector.java        # Main connector implementation
    │       ├── N8nConnectorFactory.java # Trino plugin factory
    │       ├── N8nMetadata.java         # Metadata handling
    │       ├── N8nPlugin.java           # Plugin entry point
    │       ├── N8nRecordCursor.java     # Record iteration
    │       ├── N8nRecordSet.java        # Record set management
    │       ├── N8nRecordSetProvider.java # Record set provider
    │       ├── N8nSplit.java            # Split representation
    │       ├── N8nSplitManager.java     # Split management
    │       ├── N8nTableHandle.java      # Table handle
    │       └── N8nColumnHandle.java     # Column handle
    └── test/
        └── java/com/example/trino/n8n/
            ├── N8nClientTest.java       # Client unit tests
            └── N8nRecordCursorTest.java # Cursor unit tests
```

## Features

- **Schema Discovery**: Automatically discover available schemas from n8n
- **Table Discovery**: List tables within each schema
- **Column Metadata**: Query column names and types
- **Data Reading**: Read data from n8n webhook endpoints via HTTP
- **Pagination Support**: Handle large datasets with offset/limit pagination
- **Type Mapping**: Support for VARCHAR, BIGINT, INTEGER, BOOLEAN, DOUBLE, DATE, TIMESTAMP

## Commands

### Build Commands

| Command | Description |
|---------|-------------|
| `mvn clean` | Clean the target directory |
| `mvn compile` | Compile the Java source files |
| `mvn package` | Build the JAR package |
| `mvn clean package` | Clean and build the package (full build) |

### Testing Commands

| Command | Description |
|---------|-------------|
| `mvn test` | Run all unit tests |
| `mvn test -Dtest=N8nClientTest` | Run a specific test class |
| `mvn test -Dtest=N8nClientTest#testMethod` | Run a specific test method |
| `mvn verify` | Run tests and perform verification |

### Development Commands

| Command | Description |
|---------|-------------|
| `mvn dependency:tree` | Show dependency tree |
| `mvn dependency:resolve` | Resolve all dependencies |
| `mvn help:effective-pom` | Show effective POM |

### Mock Service Commands

| Command | Description |
|---------|-------------|
| `python mock-n8n-service/server.py` | Start the mock n8n service (manual) |
| `.\mock-n8n-service\start-mock-n8n.ps1` | Start the mock n8n service (Windows) |

### Trino SQL Commands (Example Queries)

| Command | Description |
|---------|-------------|
| `SHOW CATALOGS` | List all available catalogs |
| `SHOW SCHEMAS FROM n8n` | List all schemas in the n8n connector |
| `SHOW TABLES FROM n8n.zoho` | List all tables in the zoho schema |
| `DESCRIBE n8n.zoho.customers` | Show column definitions for a table |
| `SELECT * FROM n8n.zoho.customers` | Query all data from a table |
| `SELECT * FROM n8n.shopify.orders LIMIT 10` | Query with limit |
| `SELECT name, external_id FROM n8n.zoho.customers` | Query specific columns |

## Prerequisites

- **Java 17** or higher
- **Maven 3.8+**
- **Python 3.8+** (for mock service)
- **Trino 479** (server version must match `trino.version` in pom.xml)

## Build

```bash
mvn clean package
```

The compiled JAR will be in the `target/` directory.

## Installation

1. Build the connector:
   ```bash
   mvn clean package
   ```

2. Copy the JAR and dependencies to Trino's plugin directory:
   ```bash
   mkdir -p /path/to/trino/plugins/n8n
   cp target/n8n-trino-connector-1.0.0.jar /path/to/trino/plugins/n8n/
   ```

3. Create a catalog configuration file (`n8n.properties`):
   ```properties
   connector.name=n8n
   n8n.base-url=http://localhost:5678/webhook
   ```

   For direct webhook compatibility, the connector also supports:
   ```properties
   connector.name=zoho_books
   endpoint=https://your-instance.app.n8n.cloud/webhook/zoho-books
   # fallback-endpoint=https://your-instance.app.n8n.cloud/webhook-test/zoho-books
   # n8n.schema-name=books
   # n8n.table-name=items
   ```

4. Restart Trino and verify:
   ```sql
   SHOW CATALOGS;
   ```

## Configuration

| Property | Description | Default |
|----------|-------------|---------|
| `connector.name` | Registered connector factory name (`n8n` or `zoho_books`) | - |
| `n8n.base-url` | Base URL of n8n webhook endpoint | `http://localhost:5678/webhook` |
| `endpoint` | Direct webhook URL for single-endpoint compatibility mode | - |
| `fallback-endpoint` | Backup direct webhook URL if the primary endpoint fails | - |
| `n8n.schema-name` | Schema name exposed in direct webhook mode | `books` |
| `n8n.table-name` | Optional fixed table name in direct webhook mode | inferred |
| `n8n.timeout` | HTTP request timeout in milliseconds | `30000` |

## Local Development & Testing

### Using the Mock Service

If you don't have access to a live n8n instance, use the included mock service:

1. Start the mock service:
   ```powershell
   # Windows
   .\mock-n8n-service\start-mock-n8n.ps1
   
   # Or manually
   cd mock-n8n-service
   python server.py
   ```

2. Ensure `n8n.base-url=http://localhost:5678/webhook` is set in `n8n.properties`.

3. Start Trino and query:
   ```sql
   SHOW SCHEMAS FROM n8n;
   SHOW TABLES FROM n8n.zoho;
   SELECT * FROM n8n.zoho.customers;
   SELECT * FROM n8n.shopify.orders;
   ```

### Running Tests

```bash
mvn test
```

## n8n API Contract

The n8n service must implement the following HTTP endpoints:

### Metadata Endpoints

| Endpoint | Method | Query Parameters | Response |
|----------|--------|------------------|----------|
| `/meta/schemas` | GET | - | `{"schemas": ["zoho", "tally", "shopify"]}` |
| `/meta/tables` | GET | `schema` | `{"tables": ["customers", "invoices"]}` |
| `/meta/table` | GET | `schema`, `table` | `{"schema": "...", "table": "...", "columns": [...]}` |

### Data Endpoint

| Endpoint | Method | Query Parameters | Response |
|----------|--------|------------------|----------|
| `/data` | GET | `schema`, `table` | `{"data": [...]}` |
| `/data` | GET | `schema`, `table`, `offset`, `limit` | `{"data": [...], "nextOffset": N}` |

### Supported Column Types

- `varchar` → Java String
- `bigint` → Java Long
- `integer` → Java Integer
- `boolean` → Java Boolean
- `double` → Java Double
- `date` → Java LocalDate
- `timestamp` → Java LocalDateTime

## 3. Architecture

### 3.1 High-Level Architecture

```
┌─────────────────────────────────────────────────────────────────────────┐
│                              TRINO SERVER                               │
│  ┌─────────────┐     ┌──────────────┐     ┌─────────────┐               │
│  │   Trino     │────▶│ N8nConnector │────▶│ N8nClient   │               │
│  │  (Query)    │     │  (Plugin)    │     │  (HTTP)     │               │
│  └─────────────┘     └──────────────┘     └─────────────┘               │
│                           │                     │                       │
│                           ▼                     ▼                       │
│                    ┌──────────────┐     ┌─────────────┐               │
│                    │  Metadata    │     │  n8n Webhook │               │
│                    │  (Schemas,   │     │  (REST API)  │               │
│                    │   Tables)    │     └─────────────┘               │
│                    └──────────────┘                                    │
└─────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
                         ┌─────────────────────┐
                         │   n8n Workflow      │
                         │  (Webhook Endpoint) │
                         └─────────────────────┘
```

### 3.2 Component Architecture

The connector is composed of the following core components:

```
┌────────────────────────────────────────────────────────────────────────┐
│                        N8n Connector Package                           │
├────────────────────────────────────────────────────────────────────────┤
│                                                                        │
│  ┌──────────────────┐     ┌──────────────────┐     ┌──────────────┐ │
│  │  N8nPlugin       │────▶│ N8nConnector     │────▶│ N8nConfig    │ │
│  │  (Entry Point)   │     │  (Main Class)    │     │ (Properties) │ │
│  └──────────────────┘     └──────────────────┘     └──────────────┘ │
│           │                        │                                   │
│           │                        ├──────────────────────────────┐   │
│           │                        │                              │   │
│           ▼                        ▼                              ▼   │
│  ┌──────────────────┐     ┌──────────────────┐     ┌──────────────┐ │
│  │ N8nConnector     │     │   N8nMetadata    │     │ N8nClient    │ │
│  │ Factory          │     │ (Schema/Table)   │     │ (HTTP Calls) │ │
│  └──────────────────┘     └──────────────────┘     └──────────────┘ │
│                                                                        │
│  ┌──────────────────┐     ┌──────────────────┐     ┌──────────────┐ │
│  │ N8nSplitManager  │────▶│ N8nRecordSet     │────▶│ N8nRecord     │ │
│  │ (Split Mgmt)     │     │ Provider         │     │ Cursor       │ │
│  └──────────────────┘     └──────────────────┘     └──────────────┘ │
│                                                                        │
│  ┌──────────────────┐     ┌──────────────────┐                       │
│  │ N8nTableHandle   │────▶│ N8nColumnHandle  │                       │
│  │ (Table Context)  │     │ (Column Context) │                       │
│  └──────────────────┘     └──────────────────┘                       │
│                                                                        │
└────────────────────────────────────────────────────────────────────────┘
```

### 3.3 Data Flow

```
User Query: SELECT * FROM n8n.zoho.customers
                │
                ▼
┌──────────────────────────────────────────────────────────────────────┐
│                         TRINO QUERY PROCESSING                        │
└──────────────────────────────────────────────────────────────────────┘
                │
                ▼
┌──────────────────────────────────────────────────────────────────────┐
│ 1. PARSE & PLAN                                                       │
│    - Trino parses SQL → creates execution plan                       │
│    - Identifies catalog: 'n8n', schema: 'zoho', table: 'customers'   │
└──────────────────────────────────────────────────────────────────────┘
                │
                ▼
┌──────────────────────────────────────────────────────────────────────┐
│ 2. METADATA PHASE                                                     │
│    - N8nConnector.getMetadata()                                      │
│    - N8nMetadata.listSchemaNames() → GET /meta/schemas              │
│    - N8nMetadata.listTables() → GET /meta/tables?schema=zoho         │
│    - N8nMetadata.getColumnMetadata() → GET /meta/table?schema=...  │
└──────────────────────────────────────────────────────────────────────┘
                │
                ▼
┌──────────────────────────────────────────────────────────────────────┐
│ 3. SPLIT PHASE                                                        │
│    - N8nConnector.getSplitManager()                                  │
│    - N8nSplitManager.getSplits() → creates N8nSplit                  │
│    - Split represents data partition for reading                    │
└──────────────────────────────────────────────────────────────────────┘
                │
                ▼
┌──────────────────────────────────────────────────────────────────────┐
│ 4. DATA READ PHASE                                                    │
│    - N8nConnector.getRecordSetProvider()                             │
│    - N8nRecordSetProvider.createRecordSet()                         │
│    - N8nRecordCursor.next() → GET /data?schema=zoho&table=customers │
│    - Returns rows to Trino execution engine                          │
└──────────────────────────────────────────────────────────────────────┘
                │
                ▼
┌──────────────────────────────────────────────────────────────────────┐
│ 5. RESULT RETURN                                                      │
│    - Trino assembles results                                          │
│    - Returns to client                                                │
└──────────────────────────────────────────────────────────────────────┘
```

### 3.4 Class Responsibilities

| Class | Responsibility | Key Methods |
|-------|----------------|--------------|
| `N8nPlugin` | Plugin entry point, registers the connector factory | `getConnectorFactories()` |
| `N8nConnectorFactory` | Creates connector instances | `create()` |
| `N8nConnector` | Main connector, coordinates metadata and data access | `beginTransaction()`, `getMetadata()`, `getSplitManager()`, `getRecordSetProvider()` |
| `N8nConfig` | Holds configuration properties | `getBaseUrl()`, `getAuthToken()`, etc. |
| `N8nClient` | HTTP client for n8n API communication | `getSchemas()`, `getTables()`, `getDataPage()` |
| `N8nMetadata` | Provides schema/table/column metadata | `listSchemaNames()`, `listTables()`, `getColumns()` |
| `N8nSplitManager` | Manages data splits for parallel reading | `getSplits()` |
| `N8nRecordSetProvider` | Creates record sets | `createRecordSet()` |
| `N8nRecordSet` | Represents a set of records to read | `cursor()` |
| `N8nRecordCursor` | Iterates over data rows | `next()`, `getBoolean()`, `getLong()`, `getString()` |
| `N8nTableHandle` | Represents a table in queries | Holds schema/table names |
| `N8nColumnHandle` | Represents a column in queries | Holds column name/type |

---

## 4. Component Details

### 4.1 N8nPlugin (Entry Point)

The `N8nPlugin` class is the entry point that Trino uses to load the connector. It implements the Trino `Plugin` interface.

```java
// filepath: src/main/java/com/example/trino/n8n/N8nPlugin.java
package com.example.trino.n8n;

import io.trino.spi.Plugin;
import io.trino.spi.connector.ConnectorFactory;
import java.util.Set;

public class N8nPlugin implements Plugin
{
    @Override
    public Set<ConnectorFactory> getConnectorFactories()
    {
        return Set.of(new N8nConnectorFactory());
    }
}
```

**Purpose**: Registers the `N8nConnectorFactory` with Trino's plugin system.

**Key Points**:
- Must be declared in `META-INF/services/io.trino.spi.Plugin`
- Returns a `Set` of connector factories (supports multiple connectors)
- No instance state - all configuration is handled by the factory

### 4.2 N8nConnectorFactory (Factory)

Creates instances of the main connector.

```java
// filepath: src/main/java/com/example/trino/n8n/N8nConnectorFactory.java
package com.example.trino.n8n;

import io.trino.spi.connector.Connector;
import io.trino.spi.connector.ConnectorFactory;
import io.trino.spi.session.Property;
import java.util.Map;

public class N8nConnectorFactory implements ConnectorFactory
{
    @Override
    public String getName()
    {
        return "n8n";
    }

    @Override
    public Connector create(String catalogName, Map<String, String> config)
    {
        N8nConfig n8nConfig = new N8nConfig(config);
        return new N8nConnector(n8nConfig);
    }
}
```

**Purpose**: Creates connector instances with configuration from properties file.

**Key Points**:
- `getName()` returns "n8n" - this must match `connector.name` in properties
- `create()` receives catalog name and configuration map
- Parses configuration into `N8nConfig` object

### 4.3 N8nConnector (Main Connector)

The central coordinator that manages all connector operations.

```java
// filepath: src/main/java/com/example/trino/n8n/N8nConnector.java
package com.example.trino.n8n;

import io.trino.spi.connector.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class N8nConnector implements Connector
{
    private final N8nConfig config;
    private final N8nClient client;
    private final ConcurrentMap<ConnectorTransactionHandle, N8nMetadata> transactions = new ConcurrentHashMap<>();

    public N8nConnector(N8nConfig config)
    {
        this.config = config;
        this.client = new N8nClient(config);
    }

    @Override
    public ConnectorTransactionHandle beginTransaction(IsolationLevel isolationLevel, boolean readOnly, boolean autoCommit)
    {
        ConnectorTransactionHandle handle = new N8nTransactionHandle();
        transactions.put(handle, new N8nMetadata(client));
        return handle;
    }

    @Override
    public ConnectorMetadata getMetadata(ConnectorSession session, ConnectorTransactionHandle transactionHandle)
    {
        return transactions.get(transactionHandle);
    }

    @Override
    public ConnectorSplitManager getSplitManager()
    {
        return new N8nSplitManager();
    }

    @Override
    public ConnectorRecordSetProvider getRecordSetProvider()
    {
        return new N8nRecordSetProvider(client);
    }

    @Override
    public void shutdown()
    {
        transactions.clear();
    }
}
```

**Purpose**: Coordinates metadata, splits, and record sets.

**Key Points**:
- Creates `N8nClient` for HTTP communication
- Manages transaction-aware metadata instances
- Provides split manager and record set provider

### 4.4 N8nClient (HTTP Client)

Handles all HTTP communication with the n8n webhook endpoint.

```java
// filepath: src/main/java/com/example/trino/n8n/N8nClient.java
package com.example.trino.n8n;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

public class N8nClient
{
    private final HttpClient httpClient;
    private final String baseUrl;
    private final String authToken;
    private final int readTimeoutMs;
    private final int pageSize;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public N8nClient(N8nConfig config)
    {
        this.baseUrl = config.getBaseUrl();
        this.authToken = config.getAuthToken();
        this.readTimeoutMs = config.getReadTimeoutMs();
        this.pageSize = config.getPageSize();
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(config.getConnectTimeoutMs()))
                .build();
    }

    public List<String> getSchemas()
    {
        JsonNode response = getJson("/meta/schemas");
        // ... parse and return schemas
    }

    public List<String> getTables(String schema)
    {
        JsonNode response = getJson(buildPath("/meta/tables", Map.of("schema", schema)));
        // ... parse and return tables
    }

    public N8nTableDefinition getTableDefinition(String schema, String table)
    {
        JsonNode response = getJson(buildPath("/meta/table", Map.of(
                "schema", schema,
                "table", table)));
        return objectMapper.convertValue(response, N8nTableDefinition.class);
    }

    public N8nDataPage getDataPage(String schema, String table, int offset)
    {
        JsonNode response = getJson(buildPath("/data", Map.of(
                "schema", schema,
                "table", table,
                "offset", Integer.toString(offset),
                "limit", Integer.toString(pageSize))));
        // ... parse and return data page
    }

    private JsonNode getJson(String path) { /* ... */ }
    private String buildPath(String endpoint, Map<String, String> params) { /* ... */ }
}
```

**Purpose**: Abstracts all HTTP communication with n8n.

**Key Features**:
- Uses Java's built-in `HttpClient` (Java 11+)
- Configurable timeouts
- Pagination support via offset/limit
- Optional authentication token

### 4.5 N8nMetadata (Metadata Provider)

Provides schema, table, and column information to Trino.

```java
// filepath: src/main/java/com/example/trino/n8n/N8nMetadata.java
package com.example.trino.n8n;

import io.trino.spi.connector.*;
import java.util.*;

public class N8nMetadata implements ConnectorMetadata
{
    private final N8nClient client;

    public N8nMetadata(N8nClient client)
    {
        this.client = client;
    }

    @Override
    public List<String> listSchemaNames(ConnectorSession session)
    {
        return client.getSchemas();
    }

    @Override
    public ConnectorTableHandle getTableHandle(ConnectorSession session, SchemaTableName tableName)
    {
        // Validate table exists
        String schema = tableName.getSchemaName();
        String table = tableName.getTableName();
        client.getTables(schema); // validates schema exists
        return new N8nTableHandle(schema, table);
    }

    @Override
    public ConnectorTableMetadata getTableMetadata(ConnectorSession session, ConnectorTableHandle tableHandle)
    {
        N8nTableHandle handle = (N8nTableHandle) tableHandle;
        N8nTableDefinition def = client.getTableDefinition(handle.getSchemaName(), handle.getTableName());
        // ... build table metadata
    }

    @Override
    public List<ColumnMetadata> getColumns(ConnectorSession session, ConnectorTableHandle tableHandle)
    {
        // ... return column metadata
    }
}
```

**Purpose**: Implements Trino's metadata SPI for schema/table/column discovery.

### 4.6 N8nRecordCursor (Data Reader)

Iterates over data rows returned from n8n.

```java
// filepath: src/main/java/com/example/trino/n8n/N8nRecordCursor.java
package com.example.trino.n8n;

import io.trino.spi.connector.*;
import java.util.*;

public class N8nRecordCursor implements ConnectorRecordCursor
{
    private final List<Map<String, Object>> data;
    private final List<N8nColumnHandle> columns;
    private int position = -1;

    public N8nRecordCursor(List<Map<String, Object>> data, List<N8nColumnHandle> columns)
    {
        this.data = data;
        this.columns = columns;
    }

    @Override
    public boolean advance()
    {
        position++;
        return position < data.size();
    }

    @Override
    public boolean getBoolean(int field)
    {
        return (Boolean) data.get(position).get(columns.get(field).getColumnName());
    }

    @Override
    public long getLong(int field)
    {
        return (Long) data.get(position).get(columns.get(field).getColumnName());
    }

    @Override
    public double getDouble(int field)
    {
        return (Double) data.get(position).get(columns.get(field).getColumnName());
    }

    @Override
    public String getString(int field)
    {
        return (String) data.get(position).get(columns.get(field).getColumnName());
    }

    @Override
    public Object getObject(int field)
    {
        return data.get(position).get(columns.get(field).getColumnName());
    }

    @Override
    public void close() { /* cleanup */ }
}
```

**Purpose**: Provides row-by-row access to query results.

---

## 5. API Contract (Detailed)

### 5.1 Overview

The connector communicates with n8n via HTTP REST endpoints. The n8n service must implement a specific contract that the connector expects. This section details each endpoint, its purpose, request format, and response format.

### 5.2 Endpoint Summary

| Endpoint | Method | Purpose |
|----------|--------|---------|
| `/meta/schemas` | GET | List all available schemas |
| `/meta/tables` | GET | List all tables in a schema |
| `/meta/table` | GET | Get column definitions for a table |
| `/data` | GET | Retrieve data rows from a table |

### 5.3 Metadata Endpoints

#### 5.3.1 GET /meta/schemas

**Purpose**: Retrieve list of all available schemas (databases).

**Request**:
```
GET {base-url}/meta/schemas
```

**Example Request**:
```
GET http://localhost:5678/webhook/meta/schemas
```

**Response**:
```json
{
  "schemas": ["zoho", "tally", "shopify"]
}
```

**Response Fields**:
| Field | Type | Description |
|-------|------|-------------|
| `schemas` | Array of String | List of schema names |

**Error Responses**:
| Status | Body | Cause |
|--------|------|-------|
| 500 | `{"error": "message"}` | Server error |

**Java Implementation**:
```java
public List<String> getSchemas()
{
    JsonNode response = getJson("/meta/schemas");
    JsonNode schemas = response.get("schemas");
    if (schemas == null || !schemas.isArray()) {
        throw new RuntimeException("Invalid schemas response: " + response);
    }
    return objectMapper.convertValue(schemas, List.class);
}
```

---

#### 5.3.2 GET /meta/tables

**Purpose**: Retrieve list of all tables in a specific schema.

**Request**:
```
GET {base-url}/meta/tables?schema={schema_name}
```

**Query Parameters**:
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `schema` | String | Yes | Name of the schema |

**Example Request**:
```
GET http://localhost:5678/webhook/meta/tables?schema=zoho
```

**Response**:
```json
{
  "tables": ["customers", "invoices"]
}
```

**Response Fields**:
| Field | Type | Description |
|-------|------|-------------|
| `tables` | Array of String | List of table names |

**Error Responses**:
| Status | Body | Cause |
|--------|------|-------|
| 400 | `{"error": "Missing schema parameter"}` | Schema parameter missing |
| 404 | `{"error": "Schema not found"}` | Schema doesn't exist |
| 500 | `{"error": "message"}` | Server error |

---

#### 5.3.3 GET /meta/table

**Purpose**: Retrieve column definitions for a specific table.

**Request**:
```
GET {base-url}/meta/table?schema={schema_name}&table={table_name}
```

**Query Parameters**:
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `schema` | String | Yes | Name of the schema |
| `table` | String | Yes | Name of the table |

**Example Request**:
```
GET http://localhost:5678/webhook/meta/table?schema=zoho&table=customers
```

**Response**:
```json
{
  "schema": "zoho",
  "table": "customers",
  "columns": [
    {"name": "external_id", "type": "varchar"},
    {"name": "name", "type": "varchar"},
    {"name": "active", "type": "boolean"},
    {"name": "created_date", "type": "date"}
  ]
}
```

**Response Fields**:
| Field | Type | Description |
|-------|------|-------------|
| `schema` | String | Schema name (echoed back) |
| `table` | String | Table name (echoed back) |
| `columns` | Array of Object | List of column definitions |

**Column Definition**:
| Field | Type | Description |
|-------|------|-------------|
| `name` | String | Column name |
| `type` | String | Data type (see Type Mapping section) |

**Error Responses**:
| Status | Body | Cause |
|--------|------|-------|
| 400 | `{"error": "Missing parameters"}` | Required parameters missing |
| 404 | `{"error": "Table not found"}` | Table doesn't exist |

---

### 5.4 Data Endpoints

#### 5.4.1 GET /data (Without Pagination)

**Purpose**: Retrieve all data rows from a table.

**Request**:
```
GET {base-url}/data?schema={schema_name}&table={table_name}
```

**Query Parameters**:
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `schema` | String | Yes | Name of the schema |
| `table` | String | Yes | Name of the table |

**Example Request**:
```
GET http://localhost:5678/webhook/data?schema=zoho&table=customers
```

**Response**:
```json
{
  "data": [
    {"external_id": "Z1001", "name": "ABC Traders", "active": true, "created_date": "2026-04-01"},
    {"external_id": "Z1002", "name": "Northwind Supplies", "active": false, "created_date": "2026-04-05"}
  ]
}
```

**Response Fields**:
| Field | Type | Description |
|-------|------|-------------|
| `data` | Array of Object | List of data rows |

**Data Row**: Each row is a JSON object where keys are column names and values are column values.

---

#### 5.4.2 GET /data (With Pagination)

**Purpose**: Retrieve a page of data rows with optional offset and limit.

**Request**:
```
GET {base-url}/data?schema={schema_name}&table={table_name}&offset={offset}&limit={limit}
```

**Query Parameters**:
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `schema` | String | Yes | Name of the schema |
| `table` | String | Yes | Name of the table |
| `offset` | Integer | No | Number of rows to skip |
| `limit` | Integer | No | Maximum rows to return |

**Example Request**:
```
GET http://localhost:5678/webhook/data?schema=zoho&table=customers&offset=0&limit=1000
```

**Response**:
```json
{
  "data": [
    {"external_id": "Z1001", "name": "ABC Traders", "active": true, "created_date": "2026-04-01"},
    {"external_id": "Z1002", "name": "Northwind Supplies", "active": false, "created_date": "2026-04-05"}
  ],
  "nextOffset": 1000,
  "hasMore": true
}
```

**Response Fields**:
| Field | Type | Description |
|-------|------|-------------|
| `data` | Array of Object | List of data rows |
| `nextOffset` | Integer | (Optional) Offset for next page |
| `hasMore` | Boolean | (Optional) Indicates more data exists |

**Pagination Logic**:
- If `nextOffset` is present, use it for the next page request
- If `hasMore` is true, continue fetching until hasMore is false or absent
- If both are absent, treat as single-page response

---

### 5.5 Type Mapping

The connector supports mapping between n8n data types and Trino/Java types.

#### 5.5.1 Supported Types

| n8n Type | Java Type | Trino Type | Description |
|----------|-----------|------------|-------------|
| `varchar` | `String` | `VARCHAR` | Variable-length character string |
| `bigint` | `Long` | `BIGINT` | 64-bit signed integer |
| `integer` | `Integer` | `INTEGER` | 32-bit signed integer |
| `boolean` | `Boolean` | `BOOLEAN` | True/false value |
| `double` | `Double` | `DOUBLE` | 64-bit floating point |
| `date` | `LocalDate` | `DATE` | Date without time |
| `timestamp` | `LocalDateTime` | `TIMESTAMP` | Date and time |

#### 5.5.2 Type Conversion Examples

**VARCHAR**:
```json
// n8n
{"name": "ABC Traders"}
// → Java
String name = "ABC Traders"
```

**BIGINT**:
```json
// n8n
{"customer_id": 123456789}
// → Java
Long customerId = 123456789L
```

**BOOLEAN**:
```json
// n8n
{"active": true}
// → Java
Boolean active = true
```

**DATE**:
```json
// n8n
{"created_date": "2026-04-01"}
// → Java
LocalDate createdDate = LocalDate.parse("2026-04-01")
```

**TIMESTAMP**:
```json
// n8n
{"created_at": "2026-04-21T10:15:30Z"}
// → Java
LocalDateTime createdAt = LocalDateTime.parse("2026-04-21T10:15:30Z")
```

---

### 5.6 Authentication

#### 5.6.1 Token-Based Authentication

If the n8n service requires authentication, include the token in the request header:

**Request Header**:
```
Authorization: Bearer {token}
```

**Java Implementation**:
```java
private HttpRequest.Builder addAuth(HttpRequest.Builder builder)
{
    if (authToken != null && !authToken.isEmpty()) {
        builder.header("Authorization", "Bearer " + authToken);
    }
    return builder;
}
```

---

## 6. Configuration (Detailed)

### 6.1 Configuration File Location

The connector is configured through a Trino catalog properties file. The location depends on your Trino installation:

| Installation | Configuration Location |
|--------------|----------------------|
| Standalone Trino | `$TRINO_HOME/etc/catalog/n8n.properties` |
| Docker | Mount the properties file to `/etc/trino/catalog/` |
| Trino Cloud | Configure through cloud console |

### 6.2 Configuration Properties

#### 6.2.1 Required Properties

| Property | Value | Description |
|----------|-------|-------------|
| `connector.name` | `n8n` | Must match the connector factory name |

#### 6.2.2 Optional Properties

| Property | Default | Type | Description |
|----------|---------|------|-------------|
| `n8n.base-url` | `http://localhost:5678/webhook` | String | Base URL of the n8n webhook endpoint |
| `n8n.auth-token` | (none) | String | Authentication token for secured endpoints |
| `n8n.connect-timeout-ms` | `5000` | Integer | Connection timeout in milliseconds |
| `n8n.read-timeout-ms` | `30000` | Integer | Read timeout in milliseconds |
| `n8n.page-size` | `1000` | Integer | Number of records to fetch per page |

### 6.3 Example Configurations

#### 6.3.1 Local Development

```properties
# filepath: n8n.properties
connector.name=n8n
n8n.base-url=http://localhost:5678/webhook
n8n.connect-timeout-ms=5000
n8n.read-timeout-ms=30000
n8n.page-size=1000
```

#### 6.3.2 Production with Authentication

```properties
# filepath: n8n.properties
connector.name=n8n
n8n.base-url=https://your-company.app.n8n.cloud/webhook-prod
n8n.auth-token=your_secure_token_here
n8n.connect-timeout-ms=10000
n8n.read-timeout-ms=60000
n8n.page-size=500
```

#### 6.3.3 Docker Configuration

```properties
# filepath: docker-n8n.properties
connector.name=n8n
n8n.base-url=https://abhishekpalegar.app.n8n.cloud/webhook-test
n8n.auth-token=your_token_here
n8n.connect-timeout-ms=10000
n8n.read-timeout-ms=60000
n8n.page-size=1000
```

### 6.4 Environment Variables

You can use environment variables in the configuration:

```properties
connector.name=n8n
n8n.base-url=${N8N_BASE_URL}
n8n.auth-token=${N8N_AUTH_TOKEN}
```

Then set the environment variables:

```bash
export N8N_BASE_URL=https://your-n8n-instance.app.n8n.cloud/webhook
export N8N_AUTH_TOKEN=your_token_here
```

---

## 7. Installation (Detailed)

### 7.1 Prerequisites Check

Before installing, verify all prerequisites are met:

```bash
# Check Java version
java -version
# Expected: java version "17.x.x" or higher

# Check Maven version
mvn -version
# Expected: Apache Maven 3.8.x or higher

# Check Python (for mock service)
python --version
# Expected: Python 3.8.x or higher
```

### 7.2 Build the Connector

#### Step 1: Clone or Navigate to Project

```bash
cd /path/to/n8n-trino-connector
```

#### Step 2: Clean and Build

```bash
mvn clean package
```

**Expected Output**:
```
[INFO] Building n8n-trino-connector:1.0.0
[INFO] -------------------------------------------
[INFO] COMPILATION SUCCESS
[INFO] -------------------------------------------
[INFO] Tests run: 10, Failures: 0, Errors: 0, Skipped: 0
[INFO] -------------------------------------------
[INFO] BUILD SUCCESS
[INFO] -------------------------------------------
```

#### Step 3: Verify JAR Created

```bash
ls -la target/*.jar
```

**Expected**:
```
target/n8n-trino-connector-1.0.0.jar
```

### 7.3 Install to Trino

#### Option A: Standalone Trino

1. **Create plugin directory**:
```bash
mkdir -p $TRINO_HOME/plugins/n8n
```

2. **Copy JAR**:
```bash
cp target/n8n-trino-connector-1.0.0.jar $TRINO_HOME/plugins/n8n/
```

3. **Create catalog configuration**:
```bash
mkdir -p $TRINO_HOME/etc/catalog
```

4. **Create properties file**:
```bash
cat > $TRINO_HOME/etc/catalog/n8n.properties << EOF
connector.name=n8n
n8n.base-url=http://localhost:5678/webhook
EOF
```

5. **Restart Trino**:
```bash
# Stop Trino
$TRINO_HOME/bin/launcher stop

# Start Trino
$TRINO_HOME/bin/launcher start
```

#### Option B: Docker Trino

1. **Create plugin directory**:
```bash
mkdir -p docker/trino/plugins/n8n
```

2. **Copy JAR**:
```bash
cp target/n8n-trino-connector-1.0.0.jar docker/trino/plugins/n8n/
```

3. **Create catalog configuration**:
```bash
cat > docker/trino/catalog/n8n.properties << EOF
connector.name=n8n
n8n.base-url=http://host.docker.internal:5678/webhook
EOF
```

4. **Run Docker**:
```bash
docker run -d \
  --name trino \
  -p 8080:8080 \
  -v $(pwd)/docker/trino/plugins:/usr/lib/trino/plugins \
  -v $(pwd)/docker/trino/catalog:/etc/trino/catalog \
  trinodb/trino:479
```

### 7.4 Verify Installation

#### Step 1: Check Trino is Running

```bash
curl http://localhost:8080/v1/info
```

#### Step 2: List Catalogs

```sql
SHOW CATALOGS;
```

**Expected Output**:
```
 Catalog
---------
 n8n
 system
(2 rows)
```

#### Step 3: List Schemas

```sql
SHOW SCHEMAS FROM n8n;
```

**Expected Output**:
```
 Schema
-------
 zoho
 shopify
(2 rows)
```

#### Step 4: List Tables

```sql
SHOW TABLES FROM n8n.zoho;
```

**Expected Output**:
```
  Table
---------
 customers
 invoices
(2 rows)
```

#### Step 5: Query Data

```sql
SELECT * FROM n8n.zoho.customers;
```

**Expected Output**:
```
 external_id |      name       | active | created_date
-------------+-----------------+--------+--------------
 Z1001       | ABC Traders     | true   | 2026-04-01
 Z1002       | Northwind Supplies | false | 2026-04-05
(2 rows)
```

---

## 8. Usage Guide

### 8.1 Basic Queries

#### 8.1.1 List All Schemas

```sql
SHOW SCHEMAS FROM n8n;
```

**Result**:
```
 Schema
-------
 zoho
 shopify
(2 rows)
```

#### 8.1.2 List Tables in a Schema

```sql
SHOW TABLES FROM n8n.zoho;
```

**Result**:
```
  Table
---------
 customers
 invoices
(2 rows)
```

#### 8.1.3 Describe Table Structure

```sql
DESCRIBE n8n.zoho.customers;
```

**Result**:
```
   Column    |  Type   | Comment
-------------+---------+----------
 external_id | varchar |
 name        | varchar |
 active      | boolean |
 created_date| date    |
(4 rows)
```

#### 8.1.4 Select All Data from a Table

```sql
SELECT * FROM n8n.zoho.customers;
```

**Result**:
```
 external_id |      name       | active | created_date
-------------+-----------------+--------+--------------
 Z1001       | ABC Traders     | true   | 2026-04-01
 Z1002       | Northwind Supplies | false | 2026-04-05
(2 rows)
```

### 8.2 Filtering and Projection

#### 8.2.1 Select Specific Columns

```sql
SELECT name, external_id FROM n8n.zoho.customers;
```

**Result**:
```
      name       | external_id
-----------------+-------------
 ABC Traders     | Z1001
 Northwind Supplies | Z1002
(2 rows)
```

#### 8.2.2 Filter with WHERE Clause

```sql
SELECT * FROM n8n.zoho.customers WHERE active = true;
```

**Result**:
```
 external_id |      name       | active | created_date
-------------+-----------------+--------+--------------
 Z1001       | ABC Traders     | true   | 2026-04-01
(1 row)
```

#### 8.2.3 Filter with Multiple Conditions

```sql
SELECT * FROM n8n.zoho.invoices WHERE paid = false AND amount > 100;
```

**Result**:
```
 invoice_id | customer_id | amount | paid  | created_at
-----------+-------------+--------+-------+---------------------
 INV-002    | Z1002       | 240.0  | false | 2026-04-22T08:00:00Z
(1 row)
```

#### 8.2.4 Limit Results

```sql
SELECT * FROM n8n.shopify.orders LIMIT 10;
```

### 8.3 Aggregation Queries

#### 8.3.1 Count Records

```sql
SELECT COUNT(*) FROM n8n.zoho.customers;
```

**Result**:
```
 _col0
-------
     2
(1 row)
```

#### 8.3.2 Group By

```sql
SELECT paid, COUNT(*) as count, SUM(amount) as total
FROM n8n.zoho.invoices
GROUP BY paid;
```

**Result**:
```
 paid  | count |  total
-------+-------+---------
 true  |     1 | 1540.75
 false |     1 |  240.00
(2 rows)
```

#### 8.3.3 Average

```sql
SELECT AVG(amount) as avg_amount FROM n8n.zoho.invoices;
```

### 8.4 Join Queries (Cross-Source)

One of the most powerful features is joining n8n data with other data sources:

```sql
-- Join n8n customers with invoices from another catalog
SELECT 
    c.name as customer_name,
    i.invoice_id,
    i.amount
FROM n8n.zoho.customers c
JOIN n8n.zoho.invoices i ON c.external_id = i.customer_id
WHERE i.paid = true;
```

### 8.5 Complex Queries

#### 8.5.1 Subquery

```sql
SELECT * FROM n8n.zoho.customers 
WHERE external_id IN (
    SELECT customer_id 
    FROM n8n.zoho.invoices 
    WHERE amount > 500
);
```

#### 8.5.2 With Clause (CTE)

```sql
WITH unpaid_invoices AS (
    SELECT customer_id, SUM(amount) as unpaid_amount
    FROM n8n.zoho.invoices
    WHERE paid = false
    GROUP BY customer_id
)
SELECT 
    c.name,
    u.unpaid_amount
FROM n8n.zoho.customers c
JOIN unpaid_invoices u ON c.external_id = u.customer_id;
```

---

## 9. Testing

### 9.1 Unit Tests

The project includes unit tests for core functionality.

#### 9.1.1 Run All Tests

```bash
mvn test
```

**Expected Output**:
```
-------------------------------------------------------
 T E S T S
-------------------------------------------------------
Running com.example.trino.n8n.N8nClientTest
Tests run: 5, Failures: 0, Errors: 0, Skipped: 0
Running com.example.trino.n8n.N8nRecordCursorTest
Tests run: 3, Failures: 0, Errors: 0, Skipped: 0
-------------------------------------------------------
BUILD SUCCESS
```

#### 9.1.2 Run Specific Test Class

```bash
mvn test -Dtest=N8nClientTest
```

#### 9.1.3 Run Specific Test Method

```bash
mvn test -Dtest=N8nClientTest#getSchemas
```

### 9.2 Test Coverage

| Test Class | Tests | Coverage |
|------------|-------|----------|
| `N8nClientTest` | 5 | HTTP client, API calls, error handling |
| `N8nRecordCursorTest` | 3 | Record iteration, type conversion |

### 9.3 Mock Server Testing

#### 9.3.1 Start Mock Server

```bash
# Windows
.\mock-n8n-service\start-mock-n8n.ps1

# Or manually
cd mock-n8n-service
python server.py
```

#### 9.3.2 Test Mock Server Endpoints

```bash
# Test schemas endpoint
curl http://localhost:5678/webhook/meta/schemas

# Test tables endpoint
curl "http://localhost:5678/webhook/meta/tables?schema=zoho"

# Test table definition
curl "http://localhost:5678/webhook/meta/table?schema=zoho&table=customers"

# Test data endpoint
curl "http://localhost:5678/webhook/data?schema=zoho&table=customers"
```

### 9.4 Integration Testing

#### 9.4.1 Test with Live Trino

1. Start Trino with the connector installed
2. Execute test queries:
```sql
-- Test 1: Schema discovery
SHOW SCHEMAS FROM n8n;

-- Test 2: Table discovery
SHOW TABLES FROM n8n.zoho;

-- Test 3: Column metadata
DESCRIBE n8n.zoho.customers;

-- Test 4: Data query
SELECT * FROM n8n.zoho.customers;

-- Test 5: Filtered query
SELECT * FROM n8n.zoho.invoices WHERE paid = true;
```

---

## 10. Troubleshooting

### 10.1 Common Errors and Solutions

#### Error 1: Catalog Not Found

**Error Message**:
```
Catalog 'n8n' does not exist
```

**Cause**: The connector JAR is not properly installed in Trino's plugin directory.

**Solution**:
```bash
# 1. Verify JAR exists in plugin directory
ls $TRINO_HOME/plugins/n8n/

# 2. Check properties file exists
ls $TRINO_HOME/etc/catalog/n8n.properties

# 3. Restart Trino
$TRINO_HOME/bin/launcher restart
```

---

#### Error 2: Connection Timeout

**Error Message**:
```
java.net.http.HttpConnectTimeoutException: Connection timed out
```

**Cause**: The n8n service is unreachable or taking too long to respond.

**Solution**:
```bash
# 1. Verify n8n service is running
curl http://localhost:5678/webhook/meta/schemas

# 2. Check base-url in properties
cat $TRINO_HOME/etc/catalog/n8n.properties

# 3. Increase timeout values
# In n8n.properties:
n8n.connect-timeout-ms=10000
n8n.read-timeout-ms=60000
```

---

#### Error 3: Authentication Failed

**Error Message**:
```
HTTP 401: Unauthorized
```

**Cause**: Invalid or missing authentication token.

**Solution**:
```properties
# In n8n.properties, add:
n8n.auth-token=your_valid_token
```

---

#### Error 4: Schema Not Found

**Error Message**:
```
Schema 'zoho' does not exist
```

**Cause**: The n8n service didn't return the expected schema, or the schema name is incorrect.

**Solution**:
```bash
# 1. Check available schemas
curl http://localhost:5678/webhook/meta/schemas

# 2. Verify schema name matches exactly (case-sensitive)
SHOW SCHEMAS FROM n8n;
```

---

#### Error 5: Table Not Found

**Error Message**:
```
Table 'unknown_table' does not exist
```

**Cause**: Table name doesn't exist in the specified schema.

**Solution**:
```sql
-- List available tables
SHOW TABLES FROM n8n.zoho;
```

---

#### Error 6: Column Type Mismatch

**Error Message**:
```
java.lang.ClassCastException: java.lang.String cannot be cast to java.lang.Long
```

**Cause**: The column type in n8n doesn't match the expected Java type.

**Solution**:
- Verify the column type in `/meta/table` endpoint matches the actual data type
- Check that the n8n service returns correct types in the data response

---

### 10.2 Debugging Techniques

#### 10.2.1 Enable HTTP Debugging

Add JVM arguments to enable HTTP debugging:

```bash
# In Trino's jvm.config, add:
-Djavax.net.debug=ssl:handshake
-Dhttp.debug=true
```

#### 10.2.2 Check Trino Logs

```bash
# View connector logs
tail -f $TRINO_HOME/logs/trino.log | grep n8n

# Search for errors
grep -i error $TRINO_HOME/logs/trino.log
```

#### 10.2.3 Test API Endpoints Manually

```bash
# Test all endpoints
curl http://localhost:5678/webhook/meta/schemas
curl "http://localhost:5678/webhook/meta/tables?schema=zoho"
curl "http://localhost:5678/webhook/meta/table?schema=zoho&table=customers"
curl "http://localhost:5678/webhook/data?schema=zoho&table=customers"
```

#### 10.2.4 Verify Configuration

```bash
# Check effective configuration
mvn help:effective-pom

# Check dependency tree
mvn dependency:tree
```

### 10.3 Performance Issues

| Issue | Cause | Solution |
|-------|-------|----------|
| Slow queries | Large page size | Reduce `n8n.page-size` |
| High memory | Loading too much data | Use LIMIT in queries |
| Timeout on large tables | No pagination | Ensure n8n supports pagination |

---

## 11. Development Guide

### 11.1 Project Setup

#### 11.1.1 Clone the Project

```bash
git clone <repository-url>
cd n8n-trino-connector
```

#### 11.1.2 Import into IDE

**IntelliJ IDEA**:
1. File → Open → Select project folder
2. Wait for Maven to import dependencies
3. Ensure JDK 17 is selected

**VS Code**:
1. Open the project folder
2. Install Java extensions (Extension Pack for Java)
3. Configure JDK in settings.json

### 11.2 Code Organization

```
src/
├── main/
│   └── java/
│       └── com/example/trino/n8n/
│           ├── N8nPlugin.java          # Plugin entry point
│           ├── N8nConnectorFactory.java # Connector factory
│           ├── N8nConnector.java        # Main connector
│           ├── N8nConfig.java           # Configuration
│           ├── N8nClient.java           # HTTP client
│           ├── N8nMetadata.java          # Metadata provider
│           ├── N8nSplitManager.java     # Split manager
│           ├── N8nRecordSetProvider.java # Record set provider
│           ├── N8nRecordSet.java        # Record set
│           ├── N8nRecordCursor.java      # Record cursor
│           ├── N8nTableHandle.java      # Table handle
│           ├── N8nColumnHandle.java      # Column handle
│           └── N8nSplit.java            # Split
└── test/
    └── java/
        └── com/example/trino/n8n/
            ├── N8nClientTest.java       # Client tests
            └── N8nRecordCursorTest.java # Cursor tests
```

### 11.3 Adding New Features

#### 11.3.1 Adding New Configuration Property

1. Add field to `N8nConfig.java`:
```java
private int newProperty = 1000;

public int getNewProperty() {
    return newProperty;
}
```

2. Update `N8nConnectorFactory.java` to parse the property

3. Document in README.md

#### 11.3.2 Adding New Data Type

1. Update type mapping in `N8nRecordCursor.java`:
```java
@Override
public Object getObject(int field)
{
    // Add new type handling
    N8nColumnHandle column = columns.get(field);
    switch (column.getColumnType()) {
        case "newtype":
            return parseNewType(data.get(position).get(column.getColumnName()));
        // ... existing cases
    }
}
```

2. Add to supported types in API Contract section

### 11.4 Code Style

- **Package**: `com.example.trino.n8n`
- **Naming**: PascalCase for classes, camelCase for methods/variables
- **Indentation**: 4 spaces (no tabs)
- **Line Length**: Maximum 120 characters
- **Javadoc**: Required for public APIs

### 11.5 Commit Messages

Follow conventional commits:

```
<type>(<scope>): <description>

[optional body]

[optional footer]
```

**Types**:
- `feat`: New feature
- `fix`: Bug fix
- `docs`: Documentation
- `test`: Tests
- `refactor`: Code refactoring
- `chore`: Build/tooling

**Example**:
```
feat(client): add support for custom headers

Add ability to pass custom HTTP headers to n8n API calls.
This enables integration with APIs that require specific headers.

Closes #123
```

---

## 12. Deployment

### 12.1 Build for Production

```bash
# Clean build with tests
mvn clean package -DskipTests=false

# Create JAR with all dependencies (if needed)
mvn clean package -Dassembly
```

### 12.2 Package Contents

After building, the following files are created:

```
target/
├── n8n-trino-connector-1.0.0.jar    # Main connector JAR
├── n8n-trino-connector-1.0.0-sources.jar  # Source code
└── n8n-trino-connector-1.0.0-javadoc.jar # Javadoc
```

### 12.3 Deployment Checklist

- [ ] All tests pass (`mvn test`)
- [ ] JAR is built (`mvn package`)
- [ ] Configuration file is created
- [ ] Trino is restarted
- [ ] Catalogs are verified (`SHOW CATALOGS`)
- [ ] Sample queries execute successfully

### 12.4 Docker Deployment

#### 12.4.1 Build Docker Image

```dockerfile
# filepath: Dockerfile
FROM trinodb/trino:479

# Copy connector JAR
COPY target/n8n-trino-connector-1.0.0.jar /usr/lib/trino/plugin/n8n/

# Copy catalog configuration
COPY docker-n8n.properties /etc/trino/catalog/n8n.properties
```

#### 12.4.2 Build and Run

```bash
docker build -t trino-n8n-connector .
docker run -p 8080:8080 trino-n8n-connector
```

### 12.5 Kubernetes Deployment

```yaml
# filepath: trino-n8n-deployment.yaml
apiVersion: v1
kind: Pod
metadata:
  name: trino-n8n
spec:
  containers:
  - name: trino
    image: trinodb/trino:479
    volumeMounts:
    - name: n8n-connector
      mountPath: /usr/lib/trino/plugin/n8n
    - name: n8n-catalog
      mountPath: /etc/trino/catalog
  volumes:
  - name: n8n-connector
    configMap:
      name: n8n-connector-jar
  - name: n8n-catalog
    configMap:
      name: n8n-catalog-config
```

---

## 13. Security Considerations

### 13.1 Authentication

- **Token Storage**: Store authentication tokens in secure configuration management systems
- **Environment Variables**: Use environment variables for sensitive values rather than hardcoding
- **Rotation**: Regularly rotate authentication tokens

### 13.2 Network Security

- **HTTPS**: Always use HTTPS in production environments
- **Firewall**: Restrict access to the n8n endpoint to trusted sources
- **VPN**: Consider using VPN for additional network-level security

### 13.3 Data Security

- **Input Validation**: Validate all data received from n8n
- **SQL Injection**: While the connector uses parameterized queries, always validate schema/table names
- **Sensitive Data**: Be aware of sensitive data in query results

### 13.4 Best Practices

| Practice | Description |
|----------|-------------|
| Use HTTPS | Encrypt data in transit |
| Limit Permissions | Use read-only credentials when possible |
| Audit Logs | Monitor access to sensitive data |
| Regular Updates | Keep connector and dependencies updated |

---

## 14. Future Enhancements

### 14.1 Planned Features

| Feature | Description | Status |
|---------|-------------|--------|
| **Write Support** | Enable data write operations to n8n | Planned |
| **Pushdown Filters** | Push WHERE clauses to n8n API | Planned |
| **Aggregation Pushdown** | Push aggregations to n8n | Planned |
| **Streaming Support** | Support for streaming data | Considered |
| **Caching** | Result caching for improved performance | Considered |

### 14.2 Community Contributions

Contributions are welcome! Please:

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

### 14.3 Reporting Issues

To report bugs or request features:

1. Check existing issues
2. Create a new issue with:
   - Clear title
   - Steps to reproduce
   - Expected vs actual behavior
   - Environment details

---

## 15. Reference

### 15.1 Related Documentation

| Resource | URL |
|----------|-----|
| Trino Documentation | https://trino.io/docs/current/ |
| Trino SPI | https://trino.io/docs/current/develop/spi-overview.html |
| n8n Documentation | https://docs.n8n.io/ |
| Trino Connector Tutorial | https://trino.io/docs/current/develop/connector.html |

### 15.2 Maven Commands Reference

| Command | Description |
|---------|-------------|
| `mvn clean` | Remove all generated files |
| `mvn compile` | Compile source files |
| `mvn test` | Run unit tests |
| `mvn package` | Create JAR file |
| `mvn install` | Install to local repository |
| `mvn deploy` | Deploy to remote repository |
| `mvn site` | Generate project site |
| `mvn dependency:tree` | Show dependency tree |
| `mvn help:effective-pom` | Show effective POM |

### 15.3 Trino CLI Commands

| Command | Description |
|---------|-------------|
| `trino` | Start Trino CLI |
| `trino --catalog n8n` | Connect to n8n catalog |
| `trino --schema zoho` | Use zoho schema |

### 15.4 File Locations

| File | Path |
|------|------|
| Main JAR | `target/n8n-trino-connector-1.0.0.jar` |
| Properties | `n8n.properties` |
| Mock Server | `mock-n8n-service/server.py` |
| Sample Data | `mock-n8n-service/sample-data.json` |
| Plugin Service | `src/main/resources/META-INF/services/io.trino.spi.Plugin` |

---

## 16. Appendix

### 16.1 Glossary

| Term | Definition |
|------|------------|
| **Connector** | A Trino plugin that provides access to an external data source |
| **Schema** | A logical grouping of tables (similar to database) |
| **Table** | A logical grouping of columns and rows |
| **Split** | A unit of work for reading data in parallel |
| **Cursor** | An iterator over data rows |
| **Handle** | A reference to a table or column in a query |
| **SPI** | Service Provider Interface - Trino's extension API |
| **Webhook** | HTTP callback endpoint for event-driven data |

### 16.2 Error Codes

| Code | HTTP Status | Description | Solution |
|------|-------------|-------------|----------|
| E001 | 400 | Invalid request parameters | Check query parameters |
| E002 | 401 | Authentication failed | Verify auth token |
| E003 | 403 | Access forbidden | Check permissions |
| E004 | 404 | Resource not found | Verify schema/table names |
| E005 | 500 | Internal server error | Check n8n service logs |
| E006 | 503 | Service unavailable | Ensure n8n is running |

### 16.3 Trino Version Compatibility

| Connector Version | Trino Version | Java Version |
|-------------------|---------------|---------------|
| 1.0.0 | 479 | 17 |

### 16.4 Changelog

#### Version 1.0.0 (2026-04-24)

- Initial release
- Schema and table discovery
- Column metadata retrieval
- Data reading with pagination support
- Type mapping for VARCHAR, BIGINT, INTEGER, BOOLEAN, DOUBLE, DATE, TIMESTAMP
- Mock service for local development
- Unit tests for core functionality

---

## License

This project is provided as-is for educational and development purposes.

---

*Document Version: 1.0.0*  
*Last Updated: April 24, 2026*

## Sample Data

The connector comes with sample data for testing purposes. The mock service provides the following schemas and tables:

### Schemas

| Schema | Description |
|--------|-------------|
| `zoho` | Zoho CRM data (customers, invoices) |
| `shopify` | Shopify e-commerce data (orders) |

### Tables

| Schema | Table | Columns |
|--------|-------|---------|
| `zoho` | `customers` | `external_id` (varchar), `name` (varchar), `active` (boolean), `created_date` (date) |
| `zoho` | `invoices` | `invoice_id` (varchar), `customer_id` (varchar), `amount` (double), `paid` (boolean), `created_at` (timestamp) |
| `shopify` | `orders` | `order_id` (varchar), `customer_email` (varchar), `items_count` (integer), `total_amount` (double), `created_at` (timestamp) |

### Sample Data Records

**zoho.customers:**
```json
[
  {"external_id": "Z1001", "name": "ABC Traders", "active": true, "created_date": "2026-04-01"},
  {"external_id": "Z1002", "name": "Northwind Supplies", "active": false, "created_date": "2026-04-05"}
]
```

**zoho.invoices:**
```json
[
  {"invoice_id": "INV-001", "customer_id": "Z1001", "amount": 1540.75, "paid": true, "created_at": "2026-04-21T10:15:30Z"},
  {"invoice_id": "INV-002", "customer_id": "Z1002", "amount": 240.0, "paid": false, "created_at": "2026-04-22T08:00:00Z"}
]
```

**shopify.orders:**
```json
[
  {"order_id": "ORD-1001", "customer_email": "john@example.com", "items_count": 3, "total_amount": 159.99, "created_at": "2026-04-20T14:30:00Z"}
]
```

## Configuration Properties

The connector supports the following configuration properties in `n8n.properties`:

| Property | Required | Default | Description |
|----------|----------|---------|-------------|
| `connector.name` | Yes | - | Must be set to `n8n` |
| `n8n.base-url` | Yes | `http://localhost:5678/webhook` | Base URL of the n8n webhook endpoint |
| `n8n.auth-token` | No | - | Optional authentication token for secured endpoints |
| `n8n.connect-timeout-ms` | No | `5000` | Connection timeout in milliseconds |
| `n8n.read-timeout-ms` | No | `30000` | Read timeout in milliseconds |
| `n8n.page-size` | No | `1000` | Number of records to fetch per page |

### Example Configuration

```properties
connector.name=n8n
n8n.base-url=https://your-n8n-instance.app.n8n.cloud/webhook-test
n8n.auth-token=your_secure_token_here
n8n.connect-timeout-ms=10000
n8n.read-timeout-ms=60000
n8n.page-size=500
```

## Troubleshooting

### Common Issues

| Issue | Cause | Solution |
|-------|-------|----------|
| `Catalog not found` | Connector not installed | Ensure JAR is in Trino's plugin directory |
| `Connection timeout` | n8n service unreachable | Check `n8n.base-url` and network connectivity |
| `Authentication failed` | Invalid auth token | Verify `n8n.auth-token` in properties file |
| `No schemas found` | n8n API not responding | Ensure n8n service is running and accessible |
| `Table not found` | Schema/table name mismatch | Check schema and table names in n8n |

### Debugging Steps

1. **Verify n8n service is running:**
   ```bash
   curl http://localhost:5678/webhook/meta/schemas
   ```

2. **Check connector JAR exists:**
   ```bash
   ls /path/to/trino/plugins/n8n/
   ```

3. **Review Trino logs:**
   ```bash
   tail -f /path/to/trino/logs/trino.log
   ```

4. **Test with mock service:**
   ```powershell
   .\mock-n8n-service\start-mock-n8n.ps1
   ```

## License

This project is provided as-is for educational and development purposes.
