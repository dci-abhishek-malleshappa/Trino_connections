const axios = require("axios");
const { getAccessToken } = require("../config/zoho");

const METADATA_KEYS = new Set(["code", "message", "page_context"]);
const RESPONSE_CACHE_TTL_MS = 30 * 1000;
const responseCache = new Map();
const ZOHO_BOOKS_BASE_URL = process.env.ZOHO_BOOKS_BASE_URL || "https://www.zohoapis.in/books/v3";
const zohoBooksClient = axios.create({
  baseURL: ZOHO_BOOKS_BASE_URL,
  proxy: false,
  timeout: 30000,
});

const normalizeModuleName = (moduleName) => {
  const normalized = String(moduleName || "").trim().toLowerCase();
  if (!/^[a-z][a-z_]*$/.test(normalized)) {
    return null;
  }
  return normalized;
};

const extractRecords = (payload, moduleName) => {
  if (Array.isArray(payload)) {
    return payload;
  }

  if (Array.isArray(payload?.[moduleName])) {
    return payload[moduleName];
  }

  const firstArrayEntry = Object.entries(payload || {}).find(
    ([key, value]) => !METADATA_KEYS.has(key) && Array.isArray(value)
  );

  return firstArrayEntry ? firstArrayEntry[1] : [];
};

const fetchZohoModule = async (moduleName, organizationId) => {
  const cacheKey = `${organizationId}:${moduleName}`;
  const cachedEntry = responseCache.get(cacheKey);
  const now = Date.now();

  if (cachedEntry && now < cachedEntry.expiresAt) {
    return cachedEntry.value;
  }

  const accessToken = await getAccessToken();

  const response = await zohoBooksClient.get(
    `/${moduleName}`,
    {
      params: {
        organization_id: organizationId,
      },
      headers: {
        Authorization: `Zoho-oauthtoken ${accessToken}`,
      },
    }
  );

  const normalizedPayload = {
    module: moduleName,
    organization_id: organizationId,
    data: extractRecords(response.data, moduleName),
    raw: response.data,
  };

  responseCache.set(cacheKey, {
    value: normalizedPayload,
    expiresAt: now + RESPONSE_CACHE_TTL_MS,
  });

  return normalizedPayload;
};

const getContacts = async (req, res) => {
  try {
    const payload = await fetchZohoModule("contacts", process.env.ORG_ID);
    res.json(payload.raw);
  } catch (error) {
    const details = error.response?.data || error.message;
    console.error("Contacts fetch failed:", details);
    res.status(500).json({ error: "Failed to fetch contacts", details });
  }
};

const getZohoModule = async (req, res) => {
  try {
    const moduleName = normalizeModuleName(req.params.module);
    const organizationId = req.query.organization_id || process.env.ORG_ID;

    if (!moduleName) {
      return res.status(400).json({ error: "Invalid Zoho module name" });
    }

    if (!organizationId) {
      return res.status(400).json({ error: "organization_id is required" });
    }

    const payload = await fetchZohoModule(moduleName, organizationId);
    res.json(payload);
  } catch (error) {
    const details = error.response?.data || error.message;
    console.error(`Module fetch failed for ${req.params.module}:`, details);
    res.status(500).json({ error: `Failed to fetch ${req.params.module}`, details });
  }
};

module.exports = { getContacts, getZohoModule };
