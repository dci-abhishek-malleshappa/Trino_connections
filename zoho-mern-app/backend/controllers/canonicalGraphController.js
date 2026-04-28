const {
  bootstrapCanonicalGraph,
  getCanonicalSummary,
  listCanonicalEntities,
  listSourceMappings,
} = require("../services/canonicalGraphService");

const handleError = (res, error) => {
  const statusCode = error.statusCode || 500;
  console.error("Canonical graph error:", error.message);
  res.status(statusCode).json({ error: error.message });
};

const bootstrapGraph = async (_req, res) => {
  try {
    const result = await bootstrapCanonicalGraph();
    res.status(201).json(result);
  } catch (error) {
    handleError(res, error);
  }
};

const getSummary = async (_req, res) => {
  try {
    const summary = await getCanonicalSummary();
    res.json(summary || { message: "Canonical graph not initialized" });
  } catch (error) {
    handleError(res, error);
  }
};

const getEntities = async (_req, res) => {
  try {
    const entities = await listCanonicalEntities();
    res.json({ items: entities });
  } catch (error) {
    handleError(res, error);
  }
};

const getSourceMappings = async (req, res) => {
  try {
    const mappings = await listSourceMappings(req.params.sourceKey);
    res.json({
      sourceKey: req.params.sourceKey,
      items: mappings,
    });
  } catch (error) {
    handleError(res, error);
  }
};

module.exports = {
  bootstrapGraph,
  getEntities,
  getSourceMappings,
  getSummary,
};
