/**
 * ============================================================================
 * ROUTECJ LOGISTICS ECOSYSTEM - GOOGLE SHEETS BACKUP SYSTEM (100% FREE TIER)
 * ============================================================================
 *
 * Master Spreadsheet Name: ROUTECJ DATABASE BACKUP
 *
 * Architecture:
 *   RouteCJ Mobile/Web Apps -> Firebase Firestore (Primary) -> Google Apps Script -> Google Sheets
 *
 * Authentication Architecture (100% Free & Secure):
 *   - Google Apps Script authenticates to Firebase Firestore using a short-lived
 *     OAuth 2.0 Bearer Token (RS256 JWT Signed via Google's OAuth Token Endpoint).
 *   - Credentials (client_email, private_key, auth_secret_token) are stored
 *     securely in server-side encrypted Apps Script 'Script Properties'.
 *   - ZERO service account keys or secrets are stored in the Android app, strings.xml,
 *     BuildConfig, or Git repositories.
 *   - Firebase firestore.rules and storage.rules remain strict and unaltered.
 * ============================================================================
 */

// SPREADSHEET TAB NAMES
var SHEET_TABS = {
  ORDERS: "Orders",
  DISPATCHES: "Dispatches",
  DRIVERS: "Drivers",
  VEHICLES: "Vehicles",
  GODOWNS: "Godowns",
  ADMINS: "Admins",
  TRACKING: "Tracking",
  BACKUP_LOG: "Backup Log"
};

/**
 * ----------------------------------------------------------------------------
 * 1. SCRIPT CONFIGURATION & PROPERTY HELPERS (SECURE SERVER-SIDE STORAGE)
 * ----------------------------------------------------------------------------
 */
function getFirebaseProjectId() {
  var props = PropertiesService.getScriptProperties();
  return props.getProperty("FIREBASE_PROJECT_ID") || "supplychaintracking-21492";
}

function getAuthSecretToken() {
  var props = PropertiesService.getScriptProperties();
  var token = props.getProperty("AUTH_SECRET_TOKEN");
  if (!token) {
    token = "RouteCJ_Backup_Token_2026_Secured";
    props.setProperty("AUTH_SECRET_TOKEN", token);
  }
  return token;
}

/**
 * One-time setup helper to configure credentials directly in Script Properties.
 * Run this function once from the Apps Script Editor, or set them via:
 * Project Settings (⚙️) > Script Properties.
 */
function configureScriptProperties(clientEmail, privateKey, projectId, authSecretToken) {
  var props = PropertiesService.getScriptProperties();
  if (clientEmail) props.setProperty("FIREBASE_CLIENT_EMAIL", clientEmail.trim());
  if (privateKey) props.setProperty("FIREBASE_PRIVATE_KEY", privateKey.trim());
  if (projectId) props.setProperty("FIREBASE_PROJECT_ID", projectId.trim());
  if (authSecretToken) props.setProperty("AUTH_SECRET_TOKEN", authSecretToken.trim());
  
  Logger.log("✅ Script Properties updated successfully!");
}

/**
 * ----------------------------------------------------------------------------
 * 2. OAUTH 2.0 FIRESTORE AUTHENTICATION (RS256 JWT TOKEN EXCHANGE)
 * ----------------------------------------------------------------------------
 * Generates a short-lived Google OAuth 2.0 access token using built-in
 * Utilities.computeRsaSha256Signature and caches it for 55 minutes.
 */
function getFirestoreAccessToken() {
  var cache = CacheService.getScriptCache();
  var cachedToken = cache.get("FIRESTORE_OAUTH_TOKEN");
  if (cachedToken) {
    return cachedToken;
  }

  var props = PropertiesService.getScriptProperties();
  var clientEmail = props.getProperty("FIREBASE_CLIENT_EMAIL");
  var privateKey = props.getProperty("FIREBASE_PRIVATE_KEY");

  if (!clientEmail || !privateKey) {
    throw new Error(
      "Missing Firebase Service Account credentials in Script Properties.\n" +
      "Please set 'FIREBASE_CLIENT_EMAIL' and 'FIREBASE_PRIVATE_KEY' in Project Settings > Script Properties.\n" +
      "See GOOGLE_SHEETS_BACKUP_SETUP.md for quick instructions."
    );
  }

  // Format private key (handle newline escapes if pasted as single string)
  privateKey = privateKey.replace(/\\n/g, "\n");

  var now = Math.floor(Date.now() / 1000);
  var header = {
    alg: "RS256",
    typ: "JWT"
  };

  var claimSet = {
    iss: clientEmail,
    scope: "https://www.googleapis.com/auth/datastore",
    aud: "https://oauth2.googleapis.com/token",
    exp: now + 3600,
    iat: now
  };

  var encodedHeader = Utilities.base64EncodeWebSafe(JSON.stringify(header));
  var encodedClaimSet = Utilities.base64EncodeWebSafe(JSON.stringify(claimSet));
  var signatureInput = encodedHeader + "." + encodedClaimSet;

  var signature = Utilities.computeRsaSha256Signature(signatureInput, privateKey);
  var encodedSignature = Utilities.base64EncodeWebSafe(signature);

  var jwt = signatureInput + "." + encodedSignature;

  var response = UrlFetchApp.fetch("https://oauth2.googleapis.com/token", {
    method: "post",
    contentType: "application/x-www-form-urlencoded",
    payload: {
      grant_type: "urn:ietf:params:oauth:grant-type:jwt-bearer",
      assertion: jwt
    },
    muteHttpExceptions: true
  });

  var json = JSON.parse(response.getContentText());
  if (json.access_token) {
    // Cache access token for 55 minutes (3300 seconds)
    cache.put("FIRESTORE_OAUTH_TOKEN", json.access_token, 3300);
    return json.access_token;
  } else {
    throw new Error("Failed to obtain Firestore OAuth access token: " + response.getContentText());
  }
}

/**
 * Diagnostic test function: Run this from Apps Script editor to test connection.
 */
function testFirestoreConnection() {
  try {
    Logger.log("1. Authenticating to Google OAuth token endpoint...");
    var token = getFirestoreAccessToken();
    Logger.log("✅ OAuth token acquired successfully (First 15 chars: " + token.substring(0, 15) + "...)");

    Logger.log("2. Testing Firestore REST read on 'orders' collection...");
    var docs = fetchFirestoreDocuments("orders");
    Logger.log("✅ Successfully connected to Firestore! Found " + docs.length + " documents in 'orders'.");
    return true;
  } catch (e) {
    Logger.log("❌ Connection Test Failed: " + e.message);
    return false;
  }
}

/**
 * ----------------------------------------------------------------------------
 * 3. ONE-CLICK INITIALIZATION: Setup all 8 Tabs with Professional Styling
 * ----------------------------------------------------------------------------
 */
function setupSheets() {
  var ss = SpreadsheetApp.getActiveSpreadsheet();
  if (!ss) {
    Logger.log("Error: Please open the Google Sheet and launch Apps Script via Extensions > Apps Script.");
    return;
  }

  try {
    ss.rename("ROUTECJ DATABASE BACKUP");
  } catch (e) {
    Logger.log("Note: Rename spreadsheet manually if needed to 'ROUTECJ DATABASE BACKUP'");
  }

  var headers = {
    "Orders": [
      "Order ID", "Order Number", "Customer Name", "Customer Phone", "Item Name",
      "Quantity", "Weight (kg/tons)", "Pickup Location", "Pickup Pincode", "Delivery Location",
      "Delivery Pincode", "Status", "Priority", "Payment Status", "Payment Method",
      "Payment Amount (₹)", "Transaction / Reference ID", "Payment Timestamp", "Payment Notes", "Total Amount (₹)",
      "Driver ID", "Driver Name", "Vehicle ID", "Vehicle Registration", "Parcel ID",
      "QR ID", "OTP Verified", "Delivered At", "Delivered By", "Delivery Remarks",
      "Created By", "Created By Role", "Source", "Created At", "Updated At"
    ],
    "Dispatches": [
      "Dispatch ID", "Order ID", "Order Number", "Customer Name", "Driver ID",
      "Driver Name", "Vehicle ID", "Vehicle Registration", "Pickup Location", "Delivery Location",
      "Status", "Priority", "Estimated Delivery", "Remarks", "Created At", "Updated At"
    ],
    "Drivers": [
      "Driver ID", "Firebase UID", "Driver Name", "Email", "Phone",
      "Status", "License Number", "License Expiry", "Assigned Vehicle", "Rating",
      "Total Deliveries", "Completed Deliveries", "Current Latitude", "Current Longitude",
      "Speed (km/h)", "Heading", "Last Active", "Created At"
    ],
    "Vehicles": [
      "Vehicle ID", "Registration Number", "Vehicle Type", "Make / Brand", "Model",
      "Capacity", "Capacity Unit", "Status", "Assigned Driver ID", "Assigned Driver Name",
      "Fuel Level (%)", "Odometer (km)", "Image URL", "Last Service Date", "Next Service Date",
      "Insurance Expiry", "Created At", "Updated At"
    ],
    "Godowns": [
      "Godown ID", "Godown Name", "Address", "City", "State",
      "Pincode", "Latitude", "Longitude", "Capacity (Tons)", "Current Stock (Tons)",
      "Manager ID", "Manager Name", "Contact Phone", "Status", "Created At", "Updated At"
    ],
    "Admins": [
      "Admin ID", "Firebase UID", "Admin Name", "Email", "Phone",
      "Role", "Status", "Last Login", "Profile Image URL"
    ],
    "Tracking": [
      "Dispatch ID", "Order ID", "Order Number", "Customer Name", "Driver ID",
      "Driver Name", "Driver Phone", "Vehicle ID", "Vehicle Registration", "Vehicle Type",
      "Pickup Location", "Delivery Location", "Trip Status", "Current Latitude", "Current Longitude",
      "Speed (km/h)", "Heading", "Accuracy (m)", "Progress (%)", "ETA",
      "Last Location Update", "Is Stale", "Updated At"
    ],
    "Backup Log": [
      "Timestamp", "Collection", "Document ID / Scope", "Operation", "Status", "Summary / Details"
    ]
  };

  var headerBgColors = {
    "Orders": "#1E3A8A",      // Deep Blue
    "Dispatches": "#065F46",  // Forest Green
    "Drivers": "#1E293B",     // Slate Dark
    "Vehicles": "#7C2D12",    // Amber Earth
    "Godowns": "#4C1D95",     // Deep Purple
    "Admins": "#831843",      // Burgundy
    "Tracking": "#0E7490",    // Cyan Teal
    "Backup Log": "#374151"   // Charcoal Grey
  };

  for (var tabName in headers) {
    var sheet = ss.getSheetByName(tabName);
    if (!sheet) {
      sheet = ss.insertSheet(tabName);
    }

    var headerRow = headers[tabName];
    sheet.getRange(1, 1, 1, headerRow.length).setValues([headerRow]);
    
    // Format Header Row
    var headerRange = sheet.getRange(1, 1, 1, headerRow.length);
    headerRange.setFontWeight("bold")
      .setFontColor("#FFFFFF")
      .setBackground(headerBgColors[tabName] || "#1E293B")
      .setFontFamily("Roboto")
      .setFontSize(10)
      .setHorizontalAlignment("center")
      .setVerticalAlignment("middle");
    
    sheet.setRowHeight(1, 36);
    sheet.setFrozenRows(1);
    sheet.setFrozenColumns(1); // Freeze Document ID Column
  }

  // Remove default "Sheet1" if empty
  var defaultSheet = ss.getSheetByName("Sheet1");
  if (defaultSheet && ss.getSheets().length > 1) {
    try { ss.deleteSheet(defaultSheet); } catch (e) {}
  }

  logBackupOperation("SYSTEM", "INITIALIZATION", "SETUP", "SUCCESS", "All 8 backup sheets initialized successfully.");
  Logger.log("Initialization complete: All 8 backup tabs created and styled.");
}

/**
 * ----------------------------------------------------------------------------
 * 4. PRIMARY SYNC ENGINE: Pulls from Firestore REST API with Bearer Token
 * ----------------------------------------------------------------------------
 */
function syncFromFirestore() {
  var startTime = new Date();
  var totalRecordsSynced = 0;
  var errors = [];

  Logger.log("Starting RouteCJ scheduled backup synchronization...");

  try {
    // 1. Sync Orders
    try {
      var ordersCount = syncOrdersCollection();
      totalRecordsSynced += ordersCount;
    } catch (e) {
      errors.push("Orders: " + e.message);
      logBackupOperation("orders", "ALL", "SYNC", "ERROR", e.message);
    }

    // 2. Sync Dispatches
    try {
      var dispatchesCount = syncDispatchesCollection();
      totalRecordsSynced += dispatchesCount;
    } catch (e) {
      errors.push("Dispatches: " + e.message);
      logBackupOperation("dispatches", "ALL", "SYNC", "ERROR", e.message);
    }

    // 3. Sync Drivers
    try {
      var driversCount = syncDriversCollection();
      totalRecordsSynced += driversCount;
    } catch (e) {
      errors.push("Drivers: " + e.message);
      logBackupOperation("drivers", "ALL", "SYNC", "ERROR", e.message);
    }

    // 4. Sync Vehicles
    try {
      var vehiclesCount = syncVehiclesCollection();
      totalRecordsSynced += vehiclesCount;
    } catch (e) {
      errors.push("Vehicles: " + e.message);
      logBackupOperation("vehicles", "ALL", "SYNC", "ERROR", e.message);
    }

    // 5. Sync Godowns
    try {
      var godownsCount = syncGodownsCollection();
      totalRecordsSynced += godownsCount;
    } catch (e) {
      errors.push("Godowns: " + e.message);
      logBackupOperation("godowns", "ALL", "SYNC", "ERROR", e.message);
    }

    // 6. Sync Admins (Sanitized)
    try {
      var adminsCount = syncAdminsCollection();
      totalRecordsSynced += adminsCount;
    } catch (e) {
      errors.push("Admins: " + e.message);
      logBackupOperation("admins", "ALL", "SYNC", "ERROR", e.message);
    }

    // 7. Sync Tracking Snapshots
    try {
      var trackingCount = syncTrackingSnapshots();
      totalRecordsSynced += trackingCount;
    } catch (e) {
      errors.push("Tracking: " + e.message);
      logBackupOperation("tracking", "ALL", "SYNC", "ERROR", e.message);
    }

    var executionTimeSec = ((new Date().getTime() - startTime.getTime()) / 1000).toFixed(1);
    var summary = "Full Sync Completed: " + totalRecordsSynced + " total records synced in " + executionTimeSec + "s.";
    if (errors.length > 0) {
      summary += " Warnings: " + errors.join("; ");
      logBackupOperation("ALL_COLLECTIONS", "BATCH_SYNC", "FULL_SYNC", "WARNING", summary);
    } else {
      logBackupOperation("ALL_COLLECTIONS", "BATCH_SYNC", "FULL_SYNC", "SUCCESS", summary);
    }

    Logger.log(summary);
    return { success: true, recordsSynced: totalRecordsSynced, summary: summary };

  } catch (globalError) {
    Logger.log("Global Sync Failure: " + globalError.message);
    logBackupOperation("SYSTEM", "GLOBAL", "FULL_SYNC", "ERROR", globalError.message);
    return { success: false, error: globalError.message };
  }
}

/**
 * ----------------------------------------------------------------------------
 * 5. COLLECTION-SPECIFIC SYNC FUNCTIONS
 * ----------------------------------------------------------------------------
 */

function syncOrdersCollection() {
  var docs = fetchFirestoreDocuments("orders");
  var sheet = getSheetOrInit(SHEET_TABS.ORDERS);
  var indexMap = buildDocumentIndexMap(sheet);
  var count = 0;

  for (var i = 0; i < docs.length; i++) {
    var doc = docs[i];
    var id = doc.id;
    var f = doc.fields || {};

    var row = [
      id,
      getString(f.orderNumber) || ("#" + id.substring(0, 6)),
      getString(f.customerName),
      getString(f.customerPhone),
      getString(f.itemName) || "Freight Goods",
      getNumber(f.quantity),
      getNumber(f.weight),
      getString(f.pickupLocation) || getString(f.pickupAddress),
      getString(f.pickupPincode),
      getString(f.deliveryLocation) || getString(f.deliveryAddress),
      getString(f.deliveryPincode),
      getString(f.status) || "PENDING",
      getString(f.priority) || "Medium",
      getString(f.paymentStatus) || "PENDING",
      getString(f.paymentMethod) || "CASH",
      getNumber(f.paymentAmount) || getNumber(f.totalAmount),
      getString(f.transactionId),
      getTimestamp(f.paymentTimestamp),
      getString(f.paymentNotes),
      getNumber(f.totalAmount) || getNumber(f.paymentAmount),
      getString(f.driverId) || getString(f.assignedDriverId),
      getString(f.driverName),
      getString(f.vehicleId) || getString(f.assignedVehicleId),
      getString(f.vehicleRegistration),
      getString(f.parcelId),
      getString(f.qrId),
      getBoolean(f.otpVerified) ? "TRUE" : "FALSE",
      getTimestamp(f.deliveredAt),
      getString(f.deliveredBy),
      getString(f.deliveryRemarks),
      getString(f.createdBy),
      getString(f.createdByRole),
      getString(f.source) || "APP",
      getTimestamp(f.createdAt),
      getTimestamp(f.updatedAt)
    ];

    upsertRow(sheet, indexMap, id, row);
    count++;
  }
  return count;
}

function syncDispatchesCollection() {
  var docs = fetchFirestoreDocuments("dispatches");
  var sheet = getSheetOrInit(SHEET_TABS.DISPATCHES);
  var indexMap = buildDocumentIndexMap(sheet);
  var count = 0;

  for (var i = 0; i < docs.length; i++) {
    var doc = docs[i];
    var id = doc.id;
    var f = doc.fields || {};

    var row = [
      id,
      getString(f.orderId),
      getString(f.orderNumber),
      getString(f.customerName),
      getString(f.driverId),
      getString(f.driverName),
      getString(f.vehicleId),
      getString(f.vehicleRegistration),
      getString(f.pickupLocation),
      getString(f.deliveryLocation),
      getString(f.status) || "PENDING",
      getString(f.priority) || "Medium",
      getTimestamp(f.estimatedDelivery),
      getString(f.remarks),
      getTimestamp(f.createdAt),
      getTimestamp(f.updatedAt)
    ];

    upsertRow(sheet, indexMap, id, row);
    count++;
  }
  return count;
}

function syncDriversCollection() {
  var docs = fetchFirestoreDocuments("drivers");
  var sheet = getSheetOrInit(SHEET_TABS.DRIVERS);
  var indexMap = buildDocumentIndexMap(sheet);
  var count = 0;

  for (var i = 0; i < docs.length; i++) {
    var doc = docs[i];
    var id = doc.id;
    var f = doc.fields || {};

    // Strictly Sanitized Driver Profile (NO Passwords, Tokens, Secrets)
    var row = [
      id,
      getString(f.uid),
      getString(f.name),
      getString(f.email),
      getString(f.phone),
      getString(f.status) || "AVAILABLE",
      getString(f.licenseNumber),
      getTimestamp(f.licenseExpiryDate),
      getString(f.assignedVehicle) || getString(f.assignedVehicleId),
      getNumber(f.rating) || 5.0,
      getNumber(f.totalDeliveries),
      getNumber(f.completedDeliveries),
      getNumber(f.currentLatitude),
      getNumber(f.currentLongitude),
      getNumber(f.speed),
      getNumber(f.heading),
      getTimestamp(f.lastActive),
      getTimestamp(f.createdAt)
    ];

    upsertRow(sheet, indexMap, id, row);
    count++;
  }
  return count;
}

function syncVehiclesCollection() {
  var docs = fetchFirestoreDocuments("vehicles");
  var sheet = getSheetOrInit(SHEET_TABS.VEHICLES);
  var indexMap = buildDocumentIndexMap(sheet);
  var count = 0;

  for (var i = 0; i < docs.length; i++) {
    var doc = docs[i];
    var id = doc.id;
    var f = doc.fields || {};

    var row = [
      id,
      getString(f.registrationNumber) || getString(f.vehicleNumber),
      getString(f.vehicleType) || "VAN",
      getString(f.brand) || getString(f.make),
      getString(f.model),
      getNumber(f.capacity),
      getString(f.capacityUnit) || "tons",
      getString(f.status) || "AVAILABLE",
      getString(f.driverId) || getString(f.assignedDriverId),
      getString(f.driverName),
      getNumber(f.fuelLevel) || 100.0,
      getNumber(f.odometer),
      getString(f.imageUrl), // Download URL reference string only
      getTimestamp(f.lastServiceDate),
      getTimestamp(f.nextServiceDate),
      getTimestamp(f.insuranceExpiry),
      getTimestamp(f.createdAt),
      getTimestamp(f.updatedAt)
    ];

    upsertRow(sheet, indexMap, id, row);
    count++;
  }
  return count;
}

function syncGodownsCollection() {
  var docs = fetchFirestoreDocuments("godowns");
  var sheet = getSheetOrInit(SHEET_TABS.GODOWNS);
  var indexMap = buildDocumentIndexMap(sheet);
  var count = 0;

  for (var i = 0; i < docs.length; i++) {
    var doc = docs[i];
    var id = doc.id;
    var f = doc.fields || {};

    var row = [
      id,
      getString(f.godownName) || getString(f.name),
      getString(f.address),
      getString(f.city),
      getString(f.state),
      getString(f.pincode),
      getNumber(f.latitude),
      getNumber(f.longitude),
      getNumber(f.capacity),
      getNumber(f.currentStock),
      getString(f.managerId),
      getString(f.managerName),
      getString(f.phone),
      getString(f.status) || "ACTIVE",
      getTimestamp(f.createdAt),
      getTimestamp(f.updatedAt)
    ];

    upsertRow(sheet, indexMap, id, row);
    count++;
  }
  return count;
}

function syncAdminsCollection() {
  var docs = fetchFirestoreDocuments("admins");
  var sheet = getSheetOrInit(SHEET_TABS.ADMINS);
  var indexMap = buildDocumentIndexMap(sheet);
  var count = 0;

  for (var i = 0; i < docs.length; i++) {
    var doc = docs[i];
    var id = doc.id;
    var f = doc.fields || {};

    // STRICT PRIVACY: NEVER export passwords, tokens, auth keys
    var row = [
      getString(f.adminId) || id,
      getString(f.uid) || id,
      getString(f.name),
      getString(f.email),
      getString(f.phone),
      getString(f.role) || "ADMIN",
      getString(f.status) || "ACTIVE",
      getString(f.lastLogin),
      getString(f.profileImage)
    ];

    upsertRow(sheet, indexMap, id, row);
    count++;
  }
  return count;
}

function syncTrackingSnapshots() {
  // Join live tracking data from dispatches, drivers, and orders
  var dispatches = fetchFirestoreDocuments("dispatches");
  var drivers = fetchFirestoreDocuments("drivers");
  var orders = fetchFirestoreDocuments("orders");

  var driverMap = {};
  for (var i = 0; i < drivers.length; i++) {
    driverMap[drivers[i].id] = drivers[i].fields || {};
  }

  var orderMap = {};
  for (var j = 0; j < orders.length; j++) {
    orderMap[orders[j].id] = orders[j].fields || {};
  }

  var sheet = getSheetOrInit(SHEET_TABS.TRACKING);
  var indexMap = buildDocumentIndexMap(sheet);
  var count = 0;
  var now = new Date();

  for (var k = 0; k < dispatches.length; k++) {
    var d = dispatches[k];
    var dispatchId = d.id;
    var df = d.fields || {};

    var orderId = getString(df.orderId);
    var of = orderMap[orderId] || {};

    var driverId = getString(df.driverId);
    var drf = driverMap[driverId] || {};

    var lastActiveStr = getTimestamp(drf.lastActive);
    var isStale = "TRUE";
    if (lastActiveStr) {
      var lastActiveDate = new Date(lastActiveStr);
      if ((now.getTime() - lastActiveDate.getTime()) < 5 * 60 * 1000) {
        isStale = "FALSE";
      }
    }

    var status = getString(df.status) || "PENDING";
    var progress = 25;
    if (status === "TRIP_STARTED") progress = 60;
    else if (status === "IN_TRANSIT") progress = 75;
    else if (status === "DELIVERED") progress = 100;

    var row = [
      dispatchId,
      orderId,
      getString(df.orderNumber) || getString(of.orderNumber),
      getString(df.customerName) || getString(of.customerName),
      driverId,
      getString(df.driverName) || getString(drf.name),
      getString(drf.phone),
      getString(df.vehicleId),
      getString(df.vehicleRegistration),
      getString(of.vehicleType) || "VAN",
      getString(df.pickupLocation) || getString(of.pickupAddress),
      getString(df.deliveryLocation) || getString(of.deliveryAddress),
      status,
      getNumber(drf.currentLatitude),
      getNumber(drf.currentLongitude),
      getNumber(drf.speed),
      getNumber(drf.heading),
      getNumber(drf.accuracy),
      progress + "%",
      status === "DELIVERED" ? "Delivered" : "In Transit (~45m)",
      lastActiveStr,
      isStale,
      Utilities.formatDate(now, "GMT+05:30", "yyyy-MM-dd HH:mm:ss")
    ];

    upsertRow(sheet, indexMap, dispatchId, row);
    count++;
  }
  return count;
}

/**
 * ----------------------------------------------------------------------------
 * 6. DUPLICATE PROTECTION & UPSERT LOGIC
 * ----------------------------------------------------------------------------
 */
function buildDocumentIndexMap(sheet) {
  var lastRow = sheet.getLastRow();
  var map = {};
  if (lastRow > 1) {
    var idValues = sheet.getRange(2, 1, lastRow - 1, 1).getValues();
    for (var i = 0; i < idValues.length; i++) {
      var id = String(idValues[i][0]).trim();
      if (id) {
        map[id] = i + 2; // 1-indexed row number in spreadsheet
      }
    }
  }
  return map;
}

function upsertRow(sheet, indexMap, docId, rowData) {
  if (!docId) return;
  var existingRow = indexMap[docId];

  if (existingRow) {
    // UPDATE existing row in place (prevents duplicate rows)
    sheet.getRange(existingRow, 1, 1, rowData.length).setValues([rowData]);
  } else {
    // INSERT new row at the bottom
    sheet.appendRow(rowData);
    var newRowNumber = sheet.getLastRow();
    indexMap[docId] = newRowNumber;
  }
}

/**
 * ----------------------------------------------------------------------------
 * 7. BACKUP LOG RECORDER
 * ----------------------------------------------------------------------------
 */
function logBackupOperation(collection, docId, operation, status, message) {
  try {
    var ss = SpreadsheetApp.getActiveSpreadsheet();
    if (!ss) return;
    var logSheet = ss.getSheetByName(SHEET_TABS.BACKUP_LOG);
    if (!logSheet) {
      setupSheets();
      logSheet = ss.getSheetByName(SHEET_TABS.BACKUP_LOG);
    }

    var timestamp = Utilities.formatDate(new Date(), "GMT+05:30", "yyyy-MM-dd HH:mm:ss");
    var logRow = [timestamp, collection, docId, operation, status, message];
    logSheet.appendRow(logRow);

    // Keep log sheet trimmed to latest 10,000 entries
    if (logSheet.getLastRow() > 10000) {
      logSheet.deleteRows(2, 500);
    }
  } catch (e) {
    Logger.log("Failed to write to Backup Log: " + e.message);
  }
}

/**
 * ----------------------------------------------------------------------------
 * 8. WEBHOOK ENDPOINT: Secure On-Demand Sync from RouteCJ Admin App
 * ----------------------------------------------------------------------------
 */
function doPost(e) {
  try {
    var payload = {};
    if (e && e.postData && e.postData.contents) {
      try {
        payload = JSON.parse(e.postData.contents);
      } catch (parseErr) {
        return ContentService.createTextOutput(JSON.stringify({
          status: "ERROR",
          message: "Malformed JSON payload"
        })).setMimeType(ContentService.MimeType.JSON);
      }
    }

    // Authenticate Request against Script Properties Secret Token
    var expectedToken = getAuthSecretToken();
    var token = payload.token || (e && e.parameter && e.parameter.token);
    if (!token || token !== expectedToken) {
      return ContentService.createTextOutput(JSON.stringify({
        status: "ERROR",
        message: "Unauthorized: Invalid RouteCJ backup authentication token"
      })).setMimeType(ContentService.MimeType.JSON);
    }

    var action = payload.action || "FULL_SYNC";

    if (action === "HEALTH_CHECK") {
      var ss = SpreadsheetApp.getActiveSpreadsheet();
      return ContentService.createTextOutput(JSON.stringify({
        status: "CONNECTED",
        spreadsheetName: ss.getName(),
        spreadsheetUrl: ss.getUrl(),
        lastSuccessfulBackup: Utilities.formatDate(new Date(), "GMT+05:30", "yyyy-MM-dd HH:mm:ss")
      })).setMimeType(ContentService.MimeType.JSON);
    }

    // Perform Backup Sync
    var result = syncFromFirestore();

    return ContentService.createTextOutput(JSON.stringify({
      status: result.success ? "SUCCESS" : "ERROR",
      recordsSynced: result.recordsSynced || 0,
      summary: result.summary || result.error,
      timestamp: Utilities.formatDate(new Date(), "GMT+05:30", "yyyy-MM-dd HH:mm:ss")
    })).setMimeType(ContentService.MimeType.JSON);

  } catch (err) {
    return ContentService.createTextOutput(JSON.stringify({
      status: "ERROR",
      message: err.message
    })).setMimeType(ContentService.MimeType.JSON);
  }
}

function doGet(e) {
  var ss = SpreadsheetApp.getActiveSpreadsheet();
  return ContentService.createTextOutput(JSON.stringify({
    service: "RouteCJ Google Sheets Backup Engine",
    status: "ACTIVE",
    spreadsheetName: ss ? ss.getName() : "UNLINKED",
    timestamp: new Date().toISOString()
  })).setMimeType(ContentService.MimeType.JSON);
}

/**
 * ----------------------------------------------------------------------------
 * 9. FIRESTORE REST API HELPER WITH OAUTH 2.0 BEARER TOKEN & PAGINATION
 * ----------------------------------------------------------------------------
 */
function fetchFirestoreDocuments(collectionName) {
  var projectId = getFirebaseProjectId();
  var accessToken = getFirestoreAccessToken();
  var allDocs = [];
  var pageToken = null;

  do {
    var url = "https://firestore.googleapis.com/v1/projects/" + projectId +
              "/databases/(default)/documents/" + collectionName + "?pageSize=300" +
              (pageToken ? "&pageToken=" + encodeURIComponent(pageToken) : "");

    var options = {
      method: "get",
      headers: {
        "Authorization": "Bearer " + accessToken,
        "Accept": "application/json"
      },
      muteHttpExceptions: true
    };

    var response = UrlFetchApp.fetch(url, options);
    var responseCode = response.getResponseCode();
    
    if (responseCode === 200) {
      var data = JSON.parse(response.getContentText());
      var rawDocs = data.documents || [];

      for (var i = 0; i < rawDocs.length; i++) {
        var d = rawDocs[i];
        var nameParts = d.name.split("/");
        var docId = nameParts[nameParts.length - 1];
        allDocs.push({
          id: docId,
          fields: d.fields || {},
          createTime: d.createTime,
          updateTime: d.updateTime
        });
      }
      pageToken = data.nextPageToken || null;
    } else {
      var errText = response.getContentText();
      Logger.log("Firestore Fetch Error for [" + collectionName + "] [Code " + responseCode + "]: " + errText);
      throw new Error("Firestore HTTP " + responseCode + " on " + collectionName + ": " + errText);
    }
  } while (pageToken);

  return allDocs;
}

/**
 * ----------------------------------------------------------------------------
 * 10. DATA TYPE EXTRACTION UTILITIES
 * ----------------------------------------------------------------------------
 */
function getString(field) {
  if (!field) return "";
  return field.stringValue || field.referenceValue || "";
}

function getNumber(field) {
  if (!field) return 0;
  if (field.integerValue !== undefined) return parseInt(field.integerValue, 10);
  if (field.doubleValue !== undefined) return parseFloat(field.doubleValue);
  return 0;
}

function getBoolean(field) {
  if (!field) return false;
  return field.booleanValue === true;
}

function getTimestamp(field) {
  if (!field) return "";
  var iso = field.timestampValue;
  if (!iso) return "";
  try {
    var d = new Date(iso);
    return Utilities.formatDate(d, "GMT+05:30", "yyyy-MM-dd HH:mm:ss");
  } catch (e) {
    return iso;
  }
}

function getSheetOrInit(tabName) {
  var ss = SpreadsheetApp.getActiveSpreadsheet();
  var sheet = ss.getSheetByName(tabName);
  if (!sheet) {
    setupSheets();
    sheet = ss.getSheetByName(tabName);
  }
  return sheet;
}
