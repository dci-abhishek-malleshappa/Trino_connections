const express = require("express");
const {
  bootstrapGraph,
  getEntities,
  getSourceMappings,
  getSummary,
} = require("../controllers/canonicalGraphController");

const router = express.Router();

router.post("/canonical/bootstrap", bootstrapGraph);
router.get("/canonical/summary", getSummary);
router.get("/canonical/entities", getEntities);
router.get("/canonical/mappings/:sourceKey", getSourceMappings);

module.exports = router;
