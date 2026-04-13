const express = require("express");
const router = express.Router();
const { getContacts, getZohoModule } = require("../controllers/contactController");

router.get("/contacts", getContacts);
router.get("/zoho/:module", getZohoModule);

module.exports = router;
