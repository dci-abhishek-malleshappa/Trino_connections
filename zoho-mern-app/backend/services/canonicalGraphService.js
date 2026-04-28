const canonicalSeed = require("../data/canonicalSeed");
const { assertNeo4jConfig } = require("../config/neo4j");

const authHeader = (username, password) =>
  `Basic ${Buffer.from(`${username}:${password}`).toString("base64")}`;

const executeCypher = async (statement, parameters = {}) => {
  const config = assertNeo4jConfig();
  const response = await fetch(config.url, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      Authorization: authHeader(config.username, config.password),
    },
    body: JSON.stringify({
      statements: [
        {
          statement,
          parameters,
        },
      ],
    }),
  });

  if (!response.ok) {
    const errorText = await response.text();
    const error = new Error(`Neo4j request failed with status ${response.status}: ${errorText}`);
    error.statusCode = 502;
    throw error;
  }

  const payload = await response.json();
  if (payload.errors?.length) {
    const firstError = payload.errors[0];
    const error = new Error(firstError.message || "Neo4j query failed");
    error.statusCode = 500;
    throw error;
  }

  return payload.results?.[0] || { columns: [], data: [] };
};

const recordsFromResult = (result) =>
  (result.data || []).map((row) =>
    result.columns.reduce((record, column, index) => {
      record[column] = row.row[index];
      return record;
    }, {})
  );

const bootstrapCanonicalGraph = async () => {
  await executeCypher("CREATE CONSTRAINT canonical_domain_key IF NOT EXISTS FOR (n:CanonicalDomain) REQUIRE n.key IS UNIQUE");
  await executeCypher("CREATE CONSTRAINT source_system_key IF NOT EXISTS FOR (n:SourceSystem) REQUIRE n.key IS UNIQUE");
  await executeCypher("CREATE CONSTRAINT canonical_entity_key IF NOT EXISTS FOR (n:CanonicalEntity) REQUIRE n.key IS UNIQUE");
  await executeCypher("CREATE CONSTRAINT canonical_attribute_key IF NOT EXISTS FOR (n:CanonicalAttribute) REQUIRE n.key IS UNIQUE");
  await executeCypher("CREATE CONSTRAINT canonical_measure_key IF NOT EXISTS FOR (n:CanonicalMeasure) REQUIRE n.key IS UNIQUE");
  await executeCypher("CREATE CONSTRAINT canonical_relationship_key IF NOT EXISTS FOR (n:CanonicalRelationship) REQUIRE n.key IS UNIQUE");
  await executeCypher("CREATE CONSTRAINT field_mapping_key IF NOT EXISTS FOR (n:FieldMapping) REQUIRE n.key IS UNIQUE");
  await executeCypher("CREATE CONSTRAINT canonical_constraint_key IF NOT EXISTS FOR (n:CanonicalConstraint) REQUIRE n.key IS UNIQUE");

  await executeCypher(
    `
    MERGE (domain:CanonicalDomain {key: $domain.key})
    SET domain.name = $domain.name,
        domain.description = $domain.description,
        domain.version = $domain.version

    WITH domain
    UNWIND $sources AS source
    MERGE (s:SourceSystem {key: source.key})
    SET s.name = source.name,
        s.connectorType = source.connectorType,
        s.schemaName = source.schemaName,
        s.description = source.description
    MERGE (domain)-[:HAS_SOURCE]->(s)
    `,
    {
      domain: canonicalSeed.domain,
      sources: canonicalSeed.sources,
    }
  );

  await executeCypher(
    `
    UNWIND $entities AS entity
    MERGE (e:CanonicalEntity {key: entity.key})
    SET e.name = entity.name,
        e.description = entity.description,
        e.primaryKey = entity.primaryKey,
        e.grain = entity.grain
    WITH e
    MATCH (domain:CanonicalDomain {key: $domainKey})
    MERGE (domain)-[:HAS_ENTITY]->(e)
    `,
    {
      domainKey: canonicalSeed.domain.key,
      entities: canonicalSeed.entities,
    }
  );

  await executeCypher(
    `
    UNWIND $attributes AS attribute
    MATCH (entity:CanonicalEntity {key: attribute.entityKey})
    MERGE (a:CanonicalAttribute {key: attribute.entityKey + "." + attribute.key})
    SET a.name = attribute.name,
        a.dataType = attribute.dataType,
        a.semanticType = attribute.semanticType,
        a.attributeKey = attribute.key
    MERGE (entity)-[:HAS_ATTRIBUTE]->(a)
    `,
    {
      attributes: canonicalSeed.attributes,
    }
  );

  await executeCypher(
    `
    UNWIND $measures AS measure
    MATCH (entity:CanonicalEntity {key: measure.entityKey})
    MERGE (m:CanonicalMeasure {key: measure.key})
    SET m.name = measure.name,
        m.fieldKey = measure.fieldKey,
        m.aggregation = measure.aggregation,
        m.dataType = measure.dataType
    MERGE (entity)-[:HAS_MEASURE]->(m)
    `,
    {
      measures: canonicalSeed.measures,
    }
  );

  await executeCypher(
    `
    UNWIND $relationships AS rel
    MATCH (fromEntity:CanonicalEntity {key: rel.fromEntity})
    MATCH (toEntity:CanonicalEntity {key: rel.toEntity})
    MERGE (r:CanonicalRelationship {key: rel.key})
    SET r.cardinality = rel.cardinality,
        r.description = rel.description,
        r.relationType = rel.relationType
    MERGE (fromEntity)-[:CANONICAL_RELATIONSHIP]->(r)
    MERGE (r)-[:TARGETS]->(toEntity)
    `,
    {
      relationships: canonicalSeed.relationships,
    }
  );

  await executeCypher(
    `
    UNWIND $fieldMappings AS mapping
    MATCH (source:SourceSystem {key: mapping.sourceKey})
    MATCH (entity:CanonicalEntity {key: mapping.entityKey})
    MATCH (attribute:CanonicalAttribute {key: mapping.entityKey + "." + mapping.attributeKey})
    MERGE (m:FieldMapping {key: mapping.key})
    SET m.sourceObject = mapping.sourceObject,
        m.sourceField = mapping.sourceField
    MERGE (source)-[:EXPOSES_MAPPING]->(m)
    MERGE (m)-[:MAPS_TO_ENTITY]->(entity)
    MERGE (m)-[:MAPS_TO_ATTRIBUTE]->(attribute)
    `,
    {
      fieldMappings: canonicalSeed.fieldMappings,
    }
  );

  await executeCypher(
    `
    UNWIND $constraints AS item
    MERGE (constraint:CanonicalConstraint {key: item.key})
    SET constraint.name = item.name,
        constraint.description = item.description,
        constraint.severity = item.severity
    WITH constraint
    MATCH (domain:CanonicalDomain {key: $domainKey})
    MERGE (domain)-[:HAS_CONSTRAINT]->(constraint)
    `,
    {
      domainKey: canonicalSeed.domain.key,
      constraints: canonicalSeed.constraints,
    }
  );

  return {
    message: "Canonical graph bootstrapped",
    domain: canonicalSeed.domain.key,
    sources: canonicalSeed.sources.map((source) => source.key),
    entities: canonicalSeed.entities.map((entity) => entity.key),
  };
};

const getCanonicalSummary = async () => {
  const result = await executeCypher(
    `
    MATCH (domain:CanonicalDomain {key: $domainKey})
    OPTIONAL MATCH (domain)-[:HAS_SOURCE]->(source:SourceSystem)
    WITH domain, collect(DISTINCT source.key) AS sources
    OPTIONAL MATCH (domain)-[:HAS_ENTITY]->(entity:CanonicalEntity)
    WITH domain, sources, collect(DISTINCT entity.key) AS entities
    OPTIONAL MATCH (domain)-[:HAS_CONSTRAINT]->(constraint:CanonicalConstraint)
    RETURN domain.key AS domainKey,
           domain.name AS domainName,
           domain.version AS version,
           domain.description AS description,
           sources,
           entities,
           count(DISTINCT constraint) AS constraintCount
    `,
    {
      domainKey: canonicalSeed.domain.key,
    }
  );

  return recordsFromResult(result)[0] || null;
};

const listCanonicalEntities = async () => {
  const result = await executeCypher(
    `
    MATCH (entity:CanonicalEntity)
    OPTIONAL MATCH (entity)-[:HAS_ATTRIBUTE]->(attribute:CanonicalAttribute)
    OPTIONAL MATCH (entity)-[:HAS_MEASURE]->(measure:CanonicalMeasure)
    RETURN entity.key AS key,
           entity.name AS name,
           entity.description AS description,
           entity.primaryKey AS primaryKey,
           entity.grain AS grain,
           collect(DISTINCT {
             key: attribute.attributeKey,
             name: attribute.name,
             dataType: attribute.dataType,
             semanticType: attribute.semanticType
           }) AS attributes,
           collect(DISTINCT {
             key: measure.key,
             name: measure.name,
             aggregation: measure.aggregation,
             fieldKey: measure.fieldKey
           }) AS measures
    ORDER BY entity.name
    `
  );

  return recordsFromResult(result);
};

const listSourceMappings = async (sourceKey) => {
  const result = await executeCypher(
    `
    MATCH (source:SourceSystem {key: $sourceKey})-[:EXPOSES_MAPPING]->(mapping:FieldMapping)
    MATCH (mapping)-[:MAPS_TO_ENTITY]->(entity:CanonicalEntity)
    MATCH (mapping)-[:MAPS_TO_ATTRIBUTE]->(attribute:CanonicalAttribute)
    RETURN source.key AS sourceKey,
           source.name AS sourceName,
           mapping.key AS mappingKey,
           mapping.sourceObject AS sourceObject,
           mapping.sourceField AS sourceField,
           entity.key AS entityKey,
           entity.name AS entityName,
           attribute.attributeKey AS attributeKey,
           attribute.name AS attributeName
    ORDER BY mapping.sourceObject, mapping.sourceField
    `,
    {
      sourceKey,
    }
  );

  return recordsFromResult(result);
};

module.exports = {
  bootstrapCanonicalGraph,
  getCanonicalSummary,
  listCanonicalEntities,
  listSourceMappings,
};
