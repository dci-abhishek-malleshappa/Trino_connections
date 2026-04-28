const http = require("http");
const express = require("express");
const cors = require("cors");
require("dotenv").config();

[
  "ALL_PROXY",
  "all_proxy",
  "HTTP_PROXY",
  "http_proxy",
  "HTTPS_PROXY",
  "https_proxy",
].forEach((key) => {
  delete process.env[key];
});

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

const contactRoutes = require("./routes/contactRoutes");
const canonicalGraphRoutes = require("./routes/canonicalGraphRoutes");

const app = express();
const START_PORT = Number(process.env.PORT) || 5000;
const MAX_PORT_ATTEMPTS = 10;

app.use(cors());
app.use(express.json());

app.get("/health", (_req, res) => {
  res.json({ status: "ok", port: activePort });
});

// Routes
app.use("/api", contactRoutes);
app.use("/api", canonicalGraphRoutes);

const server = http.createServer(app);
let activePort = START_PORT;

const startServer = (port, attempt = 0) => {
  activePort = port;
  server.listen(port, () => {
    console.log(`Server running on port ${port}`);
  });

  server.once("error", (error) => {
    if (error.code === "EADDRINUSE" && attempt < MAX_PORT_ATTEMPTS - 1) {
      const nextPort = port + 1;
      console.warn(`Port ${port} is in use, trying ${nextPort}...`);
      setTimeout(() => startServer(nextPort, attempt + 1), 100);
      return;
    }

    console.error("Server startup failed:", error);
    process.exit(1);
  });
};

startServer(START_PORT);

process.on("uncaughtException", (error) => {
  console.error("Uncaught exception:", error);
});

process.on("unhandledRejection", (reason) => {
  console.error("Unhandled rejection:", reason);
});

const shutdown = (signal) => {
  console.log(`Received ${signal}, shutting down server...`);
  server.close(() => {
    process.exit(0);
  });
};

process.on("SIGINT", () => shutdown("SIGINT"));
process.on("SIGTERM", () => shutdown("SIGTERM"));
