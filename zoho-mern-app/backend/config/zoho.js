const axios = require("axios");
require("dotenv").config();

let cachedAccessToken = null;
let accessTokenExpiresAt = 0;
const PROXY_ENV_KEYS = [
  "ALL_PROXY",
  "all_proxy",
  "HTTP_PROXY",
  "http_proxy",
  "HTTPS_PROXY",
  "https_proxy",
];

for (const key of PROXY_ENV_KEYS) {
  delete process.env[key];
}

const noProxyEntries = new Set(
  String(process.env.NO_PROXY || process.env.no_proxy || "")
    .split(",")
    .map((value) => value.trim())
    .filter(Boolean)
);

[
  "localhost",
  "127.0.0.1",
  "::1",
  "accounts.zoho.in",
  "www.zohoapis.in",
  ".zoho.in",
].forEach((value) => noProxyEntries.add(value));

process.env.NO_PROXY = Array.from(noProxyEntries).join(",");
process.env.no_proxy = process.env.NO_PROXY;

const ZOHO_ACCOUNTS_BASE_URL = process.env.ZOHO_ACCOUNTS_BASE_URL || "https://accounts.zoho.in";
const zohoAuthClient = axios.create({
  baseURL: ZOHO_ACCOUNTS_BASE_URL,
  proxy: false,
  timeout: 30000,
});

const getAccessToken = async () => {
  const now = Date.now();
  if (cachedAccessToken && now < accessTokenExpiresAt) {
    return cachedAccessToken;
  }

  try {
    const response = await zohoAuthClient.post(
      "/oauth/v2/token",
      null,
      {
        params: {
          refresh_token: process.env.REFRESH_TOKEN,
          client_id: process.env.CLIENT_ID,
          client_secret: process.env.CLIENT_SECRET,
          grant_type: "refresh_token",
        },
      }
    );

    cachedAccessToken = response.data.access_token;
    accessTokenExpiresAt = now + 50 * 60 * 1000;

    return cachedAccessToken;
  } catch (error) {
    console.error("Error refreshing token:", error.response?.data || error.message);
    throw error;
  }
};

module.exports = { getAccessToken };
