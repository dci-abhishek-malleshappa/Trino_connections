require("dotenv").config();

const getNeo4jConfig = () => ({
  url: process.env.NEO4J_HTTP_URL || "http://localhost:7474/db/neo4j/tx/commit",
  username: process.env.NEO4J_USERNAME || "",
  password: process.env.NEO4J_PASSWORD || "",
});

const assertNeo4jConfig = () => {
  const config = getNeo4jConfig();

  if (!config.username || !config.password) {
    const error = new Error("Neo4j credentials are missing. Set NEO4J_USERNAME and NEO4J_PASSWORD.");
    error.statusCode = 503;
    throw error;
  }

  return config;
};

module.exports = {
  assertNeo4jConfig,
  getNeo4jConfig,
};
