const axios = require("axios");
require("dotenv").config();

let cachedAccessToken = null;
let accessTokenExpiresAt = 0;

const getAccessToken = async () => {
  const now = Date.now();
  if (cachedAccessToken && now < accessTokenExpiresAt) {
    return cachedAccessToken;
  }

  try {
    const response = await axios.post(
      "https://accounts.zoho.in/oauth/v2/token",
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
    console.error("Error refreshing token:", error.response?.data);
    throw error;
  }
};

module.exports = { getAccessToken };
