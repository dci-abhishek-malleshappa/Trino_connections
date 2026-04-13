# Tally Trino Connector Setup and Implementation Guide

## Overview

This document explains what was built, how it works, how it was deployed, and what was verified for the custom Trino connector project:

`tally-trino-connector`

The connector allows Trino to query Tally through an HTTP/XML integration layer.

The high-level flow is:

```text
Trino SQL -> Custom Trino Connector -> Java HTTP/XML client -> Tally
```

## Goal

The goal was to build a minimal but working custom Trino connector using:

- Java
- Maven
- Trino SPI
- HTTP/XML communication with Tally
 
Version 1 scope:

- Read-only connector
- One schema: `default`
- One table: `ledgers`
- Supported operations:
  - `SHOW SCHEMAS`
  - `SHOW TABLES`
  - `DESCRIBE tally.default.ledgers`
  - `SELECT * FROM tally.default.ledgers`

## Project Structure

Project root:

`e:\AI\data engineering\kafka\java\tally-trino-connector`

Important files:

- `pom.xml`
- `src/main/java/com/example/trino/tally/TallyPlugin.java`
- `src/main/java/com/example/trino/tally/TallyConnectorFactory.java`
- `src/main/java/com/example/trino/tally/TallyConnector.java`
- `src/main/java/com/example/trino/tally/TallyMetadata.java`
- `src/main/java/com/example/trino/tally/TallySplitManager.java`
- `src/main/java/com/example/trino/tally/TallySplit.java`
- `src/main/java/com/example/trino/tally/TallyRecordSetProvider.java`
- `src/main/java/com/example/trino/tally/TallyRecordSet.java`
- `src/main/java/com/example/trino/tally/TallyRecordCursor.java`
- `src/main/java/com/example/trino/tally/TallyConfig.java`
- `src/main/java/com/example/trino/tally/TallyClient.java`
- `src/main/java/com/example/trino/tally/TallyXmlClient.java`
- `src/main/java/com/example/trino/tally/TallyXmlParser.java`
- `src/main/java/com/example/trino/tally/LedgerRow.java`
- `src/main/java/com/example/trino/tally/TallyTableHandle.java`
- `src/main/java/com/example/trino/tally/TallyColumnHandle.java`
- `src/main/java/com/example/trino/tally/TallyHandleResolver.java`
- `src/main/resources/META-INF/services/io.trino.spi.Plugin`
- `docker/tally.properties`

Built artifact:

- `target/tally-trino-connector-1.0-SNAPSHOT.jar`

## What Was Implemented

### 1. Trino Plugin Registration

`TallyPlugin` registers the connector factory with Trino.

This is how Trino discovers the connector plugin during startup.

### 2. Connector Factory

`TallyConnectorFactory` creates the connector instance and reads configuration properties:

- `endpoint`
- `company`

It also creates:

- `TallyXmlParser`
- `TallyXmlClient`
 
### 3. Connector Core

`TallyConnector` returns the main SPI components required by Trino:

- metadata
- split manager
- record set provider

### 4. Metadata

`TallyMetadata` hardcodes:

- schema: `default`
- table: `ledgers`

Columns:

- `ledger_name` as `VARCHAR`
- `parent_group` as `VARCHAR`
- `closing_balance` as `DOUBLE`

This metadata layer is what makes these commands work:

```sql
SHOW SCHEMAS FROM tally;
SHOW TABLES FROM tally.default;
DESCRIBE tally.default.ledgers;
```

### 5. Split Management

`TallySplitManager` returns a single split for every scan.

That means each query against the table performs one logical fetch from Tally in this first version.

### 6. Record Reading Path

The following classes expose Java rows to Trino:

- `TallyRecordSetProvider`
- `TallyRecordSet`
- `TallyRecordCursor`

They fetch rows from `TallyClient.getLedgers()` and expose them column-by-column to Trino.

### 7. Tally HTTP/XML Integration

`TallyClient` defines:

```java
List<LedgerRow> getLedgers()
```

`TallyXmlClient`:

- sends an HTTP POST request
- sends an XML body
- receives an XML response
- passes the response to `TallyXmlParser`

`TallyXmlParser`:

- parses XML
- maps XML nodes into `LedgerRow`
- returns `List<LedgerRow>`

### 8. Row Model

`LedgerRow` contains:

- `ledgerName`
- `parentGroup`
- `closingBalance`

## Build Tooling Setup

To build the connector locally, Java 17 and Maven were installed.

### Installed Tools

- Java 17
- Maven 3.9.9

Configured environment variables:

- `JAVA_HOME = C:\Program Files\Eclipse Adoptium\jdk-17.0.18.8-hotspot`
- `MAVEN_HOME = C:\Users\manju\AppData\Local\Programs\Apache\apache-maven-3.9.9`

## Maven Build

The project was built using:

```powershell
cd "e:\AI\data engineering\kafka\java\tally-trino-connector"
mvn clean package
```

### Build Issue Found

The initial build failed because the code used `ConnectorHandleResolver`, but the selected Trino SPI version (`431`) does not expose that API in the same way as older versions.

### Fix Applied

The code was updated to align with Trino 431:

- old resolver wiring was removed from `TallyConnectorFactory`
- `TallyHandleResolver` was kept as a placeholder class only

### Final Build Result

Build succeeded and produced:

`target/tally-trino-connector-1.0-SNAPSHOT.jar`

## Docker-Based Trino Deployment

Trino is running inside Docker.

Detected container:

`gallant_napier`

### Catalog File

A Trino catalog properties file was created:

`docker/tally.properties`

Contents:

```properties
connector.name=tally
endpoint=http://host.docker.internal:9000
company=
```

Why `host.docker.internal` was used:

- Tally is expected to run on the Windows host machine
- Trino is running inside a Docker container
- `localhost` from inside the container would point to the container itself, not the host

The catalog file was copied into the container at:

`/etc/trino/catalog/tally.properties`

## Initial Docker Runtime Issue

After restart, Trino failed with:

```text
No factory for connector 'tally'
```

### Root Cause

The catalog file existed, but the custom plugin jar had not yet been placed in Trino's plugin directory.

Trino had a catalog named `tally`, but no connector factory named `tally` was loaded.

## Fix for Docker Runtime Issue

The built jar was copied into the Trino plugin directory inside the container:

`/usr/lib/trino/plugin/tally/`

After that, the container was started again and Trino was able to recognize the custom connector.

## What Was Verified

### Metadata Verification

This query worked:

```sql
SHOW TABLES FROM tally.default;
```

Result:

- `ledgers`

This confirms:

- the plugin is loading
- the connector is registered
- the metadata implementation is working

### Query Execution Verification

This query also executed successfully:

```sql
SELECT * FROM tally.default.ledgers;
```

The result returned zero rows, but the query completed successfully without connector errors.

This confirms:

- the connector scan path is working
- split generation is working
- record set creation is working
- Trino can call into the Tally client successfully enough to complete the query pipeline

## Current State

### Working

- custom connector project created
- Maven build successful
- jar packaging successful
- plugin deployed into Docker Trino
- catalog deployed into Docker Trino
- Trino starts successfully with the connector
- `SHOW TABLES FROM tally.default` works
- `SELECT * FROM tally.default.ledgers` executes

### Not Yet Complete

The connector currently returns no ledger rows.

That means the remaining work is in the Tally integration side, not the Trino SPI plumbing.

Most likely causes:

- Tally is returning an empty response
- the XML request envelope is not correct for the Tally environment
- the XML parser tag names do not match the actual response structure from Tally

## Recommended Next Step

The next recommended debugging step is:

1. log the raw XML response returned by Tally
2. inspect the actual response structure
3. update `TallyXmlParser` to match the real XML tags
4. rerun:

```sql
SELECT * FROM tally.default.ledgers;
```

## Key Files and Their Responsibilities

### SPI and Plugin Layer

- `TallyPlugin.java`
  Registers the connector factory.

- `TallyConnectorFactory.java`
  Builds connector components from catalog config.

- `TallyConnector.java`
  Returns metadata, split manager, and record set provider.

### Metadata and Handles

- `TallyMetadata.java`
  Hardcoded schema, table, and columns.

- `TallyTableHandle.java`
  Table handle representation.

- `TallyColumnHandle.java`
  Column handle representation.

- `TallyHandleResolver.java`
  Placeholder kept for compatibility structure and future extension.

### Query Execution

- `TallySplitManager.java`
  Creates one split per scan.

- `TallySplit.java`
  Split model.

- `TallyRecordSetProvider.java`
  Creates record sets for scans.

- `TallyRecordSet.java`
  Supplies cursor and column types.

- `TallyRecordCursor.java`
  Maps Java row values to Trino row access methods.

### Tally Integration

- `TallyConfig.java`
  Stores endpoint and company config.

- `TallyClient.java`
  Defines the client interface.

- `TallyXmlClient.java`
  Performs the HTTP/XML call to Tally.

- `TallyXmlParser.java`
  Parses XML into Java rows.

- `LedgerRow.java`
  Ledger data model.

### Build and Deployment

- `pom.xml`
  Maven build configuration and dependencies.

- `src/main/resources/META-INF/services/io.trino.spi.Plugin`
  Registers the plugin class for Trino discovery.

- `docker/tally.properties`
  Trino catalog configuration for the custom connector.

## Commands Used During the Process

### Build

```powershell
mvn clean package
```

### Copy Catalog File

```powershell
docker cp tally.properties <container>:/etc/trino/catalog/tally.properties
```

### Copy Plugin Jar

```powershell
docker cp tally-trino-connector-1.0-SNAPSHOT.jar <container>:/usr/lib/trino/plugin/tally/
```

### Restart or Start Container

```powershell
docker restart <container>
docker start <container>
```

### Validation Queries

```sql
SHOW SCHEMAS FROM tally;
SHOW TABLES FROM tally.default;
DESCRIBE tally.default.ledgers;
SELECT * FROM tally.default.ledgers;
```

## Final Summary

A complete custom Trino connector pipeline was successfully built and deployed:

- connector code written in Java using Trino SPI
- packaged with Maven
- deployed into Docker-based Trino
- catalog configured
- plugin loaded successfully
- metadata queries working
- scan execution path working

The remaining work is limited to fetching and parsing actual ledger rows from Tally's XML response.
