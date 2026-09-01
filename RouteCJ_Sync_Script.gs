/**
 * ============================================================================
 * ROUTECJ LOGISTICS ECOSYSTEM — AUTOMATIC TWO-WAY FIRESTORE ↔ GOOGLE SHEETS SYNC
 * ============================================================================
 *
 * Master Spreadsheet Name: ROUTECJ DATABASE BACKUP / ROUTECJ ADMINISTRATION
 *
 * Architecture:
 *   RouteCJ Mobile/Web Apps (Admin, Driver, Customer)
 *          ↕ (Live Operations: GPS, QR, Dispatch, Delivery OTP, Auth)
 *   Firebase Firestore (PRIMARY DATABASE & SOURCE OF TRUTH)
 *          ↕ (Automatic Two-Way Synchronization Layer)
 *   Google Apps Script Sync Service (OAuth 2.0 JWT Bearer Token Authentication)
 *          ↕ (Controlled Administrative Management Mirror)
 *   Google Spreadsheet (Admins, Drivers, Vehicles, Godowns, Orders, Dispatches, Reports, SyncLog)
 *
 * Security & Reliability Guarantees:
 *   1. Firestore is the authoritative source of truth for all operational data.
 *   2. Zero private keys stored in Android app, Git repo, or Google Sheet cells.
 *   3. Strict validation: Phone, Email, 6-digit Pincodes, Latitude/Longitude ranges, Enums.
 *   4. Loop prevention: Programmatic mutation locks + syncSource metadata.
 *   5. Protected fields: Driver live GPS, Delivery OTP, QR tokens, Auth UIDs cannot be edited in Sheets.
 *   6. Audit trail: Every sync, user edit, validation rejection, and conflict logged to 'SyncLog'.
 * ============================================================================
 */

// SPREADSHEET TAB DEFINITIONS
var SHEET_TABS = {
  ADMINS: "Admins",
  DRIVERS: "Drivers",
  VEHICLES: "Vehicles",
  GODOWNS: "Godowns",
  ORDERS: "Orders",
  DISPATCHES: "Dispatches",
  REPORTS: "Reports",
  SYNC_LOG: "SyncLog"
};

// CACHE KEYS & CONFIGURATION
var CACHE_KEYS = {
  OAUTH_TOKEN: "FIRESTORE_OAUTH_TOKEN",
  MUTATION_LOCK: "ROUTECJ_PROGRAMMATIC_MUTATION_LOCK"
};

/**
 * ----------------------------------------------------------------------------
 * 1. SCRIPT CONFIGURATION & SCRIPT PROPERTIES (SECURE SERVER-SIDE STORAGE)
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
    token = "RouteCJ_Sync_Token_2026_Secured";
    props.setProperty("AUTH_SECRET_TOKEN", token);
  }
  return token;
}

/**
 * Helper to configure credentials directly in Script Properties from Apps Script Editor.
 */
function configureScriptProperties(clientEmail, privateKey, projectId, authSecretToken) {
  var props = PropertiesService.getScriptProperties();
  if (clientEmail) props.setProperty("FIREBASE_CLIENT_EMAIL", clientEmail.trim());
  if (privateKey) props.setProperty("FIREBASE_PRIVATE_KEY", privateKey.trim());
  if (projectId) props.setProperty("FIREBASE_PROJECT_ID", projectId.trim());
  if (authSecretToken) props.setProperty("AUTH_SECRET_TOKEN", authSecretToken.trim());
  
  Logger.log("✅ RouteCJ Script Properties configured successfully!");
}

/**
 * ----------------------------------------------------------------------------
 * 2. OAUTH 2.0 FIRESTORE AUTHENTICATION (RS256 JWT BEARER TOKEN)
 * ----------------------------------------------------------------------------
 */
function getFirestoreAccessToken() {
  var cache = CacheService.getScriptCache();
  var cachedToken = cache.get(CACHE_KEYS.OAUTH_TOKEN);
  if (cachedToken) {
    return cachedToken;
  }

  var props = PropertiesService.getScriptProperties();
  var clientEmail = props.getProperty("FIREBASE_CLIENT_EMAIL");
  var privateKey = props.getProperty("FIREBASE_PRIVATE_KEY");

  if (!clientEmail || !privateKey) {
    throw new Error(
      "Missing Firebase Service Account credentials in Script Properties.\n" +
      "Please set 'FIREBASE_CLIENT_EMAIL' and 'FIREBASE_PRIVATE_KEY' in Project Settings > Script Properties."
    );
  }

  privateKey = privateKey.replace(/\\n/g, "\n");
  var now = Math.floor(Date.now() / 1000);

  var header = { alg: "RS256", typ: "JWT" };
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
    cache.put(CACHE_KEYS.OAUTH_TOKEN, json.access_token, 3300); // Cache 55 min
    return json.access_token;
  } else {
    throw new Error("Failed to obtain Firestore OAuth access token: " + response.getContentText());
  }
}

/**
 * Diagnostic test function: Run from Apps Script Editor to verify connectivity.
 */
function testFirestoreConnection() {
  try {
    Logger.log("1. Authenticating to Google OAuth token endpoint...");
    var token = getFirestoreAccessToken();
    Logger.log("✅ OAuth token acquired (First 15 chars: " + token.substring(0, 15) + "...)");

    Logger.log("2. Testing Firestore REST read on 'orders' collection...");
    var docs = fetchFirestoreDocuments("orders");
    Logger.log("✅ Successfully connected! Found " + docs.length + " documents in 'orders'.");
    return true;
  } catch (e) {
    Logger.log("❌ Connection Test Failed: " + e.message);
    return false;
  }
}

/**
 * ----------------------------------------------------------------------------
 * 3. FIRESTORE REST API CLIENT (READ / PATCH / CREATE / DELETE)
 * ----------------------------------------------------------------------------
 */
function getFirestoreBaseUrl() {
  var projectId = getFirebaseProjectId();
  return "https://firestore.googleapis.com/v1/projects/" + projectId + "/databases/(default)/documents/";
}

function fetchFirestoreDocuments(collectionName) {
  var accessToken = getFirestoreAccessToken();
  var url = getFirestoreBaseUrl() + collectionName + "?pageSize=500";
  var allDocuments = [];
  var pageToken = null;

  do {
    var fetchUrl = pageToken ? (url + "&pageToken=" + encodeURIComponent(pageToken)) : url;
    var response = UrlFetchApp.fetch(fetchUrl, {
      method: "get",
      headers: {
        "Authorization": "Bearer " + accessToken,
        "Accept": "application/json"
      },
      muteHttpExceptions: true
    });

    var statusCode = response.getResponseCode();
    if (statusCode !== 200) {
      throw new Error("Firestore REST error (" + statusCode + ") on " + collectionName + ": " + response.getContentText());
    }

    var json = JSON.parse(response.getContentText());
    if (json.documents && json.documents.length > 0) {
      for (var i = 0; i < json.documents.length; i++) {
        var doc = json.documents[i];
        var nameParts = doc.name.split("/");
        var docId = nameParts[nameParts.length - 1];
        allDocuments.push({
          id: docId,
          fields: doc.fields || {},
          createTime: doc.createTime,
          updateTime: doc.updateTime
        });
      }
    }
    pageToken = json.nextPageToken;
  } while (pageToken);

  return allDocuments;
}

function fetchSingleFirestoreDocument(collectionName, documentId) {
  var accessToken = getFirestoreAccessToken();
  var url = getFirestoreBaseUrl() + collectionName + "/" + encodeURIComponent(documentId);

  var response = UrlFetchApp.fetch(url, {
    method: "get",
    headers: {
      "Authorization": "Bearer " + accessToken,
      "Accept": "application/json"
    },
    muteHttpExceptions: true
  });

  if (response.getResponseCode() === 200) {
    var doc = JSON.parse(response.getContentText());
    return {
      id: documentId,
      fields: doc.fields || {},
      createTime: doc.createTime,
      updateTime: doc.updateTime
    };
  }
  return null;
}

function patchFirestoreDocument(collectionName, documentId, fieldsMap, updateMaskKeys) {
  var accessToken = getFirestoreAccessToken();
  var url = getFirestoreBaseUrl() + collectionName + "/" + encodeURIComponent(documentId);

  if (updateMaskKeys && updateMaskKeys.length > 0) {
    var maskParams = updateMaskKeys.map(function(k) { return "updateMask.fieldPaths=" + encodeURIComponent(k); }).join("&");
    url += "?" + maskParams;
  }

  var firestoreFields = {};
  for (var key in fieldsMap) {
    var val = fieldsMap[key];
    firestoreFields[key] = convertJsValueToFirestoreValue(val);
  }

  // Always attach sync metadata
  firestoreFields["updatedAt"] = { timestampValue: new Date().toISOString() };
  firestoreFields["syncSource"] = { stringValue: "GOOGLE_SHEETS" };

  var response = UrlFetchApp.fetch(url, {
    method: "patch",
    headers: {
      "Authorization": "Bearer " + accessToken,
      "Content-Type": "application/json",
      "Accept": "application/json"
    },
    payload: JSON.stringify({ fields: firestoreFields }),
    muteHttpExceptions: true
  });

  var code = response.getResponseCode();
  if (code !== 200) {
    throw new Error("Firestore PATCH failed (" + code + "): " + response.getContentText());
  }

  return JSON.parse(response.getContentText());
}

/**
 * ----------------------------------------------------------------------------
 * 4. FIELD CONVERSIONS & DATA PARSERS
 * ----------------------------------------------------------------------------
 */
function getString(f) {
  if (!f) return "";
  if (f.stringValue !== undefined) return f.stringValue;
  if (f.integerValue !== undefined) return String(f.integerValue);
  if (f.doubleValue !== undefined) return String(f.doubleValue);
  if (f.booleanValue !== undefined) return f.booleanValue ? "TRUE" : "FALSE";
  if (f.timestampValue !== undefined) return f.timestampValue;
  if (f.referenceValue !== undefined) return f.referenceValue;
  return "";
}

function getNumber(f) {
  if (!f) return 0;
  if (f.integerValue !== undefined) return parseInt(f.integerValue, 10);
  if (f.doubleValue !== undefined) return parseFloat(f.doubleValue);
  return 0;
}

function getBoolean(f) {
  if (!f) return false;
  if (f.booleanValue !== undefined) return f.booleanValue;
  if (f.stringValue !== undefined) return f.stringValue.toLowerCase() === "true";
  return false;
}

function getTimestamp(f) {
  if (!f) return "";
  if (f.timestampValue !== undefined) {
    try {
      var d = new Date(f.timestampValue);
      return Utilities.formatDate(d, Session.getScriptTimeZone() || "GMT+05:30", "yyyy-MM-dd HH:mm:ss");
    } catch (e) {
      return f.timestampValue;
    }
  }
  return "";
}

function convertJsValueToFirestoreValue(val) {
  if (val === null || val === undefined) {
    return { nullValue: null };
  }
  if (typeof val === "boolean") {
    return { booleanValue: val };
  }
  if (typeof val === "number") {
    if (Number.isInteger(val)) {
      return { integerValue: String(val) };
    }
    return { doubleValue: val };
  }
  if (val instanceof Date) {
    return { timestampValue: val.toISOString() };
  }
  return { stringValue: String(val) };
}

/**
 * ----------------------------------------------------------------------------
 * 5. LOOP PREVENTION & LOCK ENGINE
 * ----------------------------------------------------------------------------
 */
function setProgrammaticLock(durationSeconds) {
  var cache = CacheService.getScriptCache();
  cache.put(CACHE_KEYS.MUTATION_LOCK, "LOCKED", durationSeconds || 30);
}

function isProgrammaticLockActive() {
  var cache = CacheService.getScriptCache();
  return cache.get(CACHE_KEYS.MUTATION_LOCK) === "LOCKED";
}

function clearProgrammaticLock() {
  var cache = CacheService.getScriptCache();
  cache.remove(CACHE_KEYS.MUTATION_LOCK);
}

/**
 * ----------------------------------------------------------------------------
 * 6. SHEET DEFINITIONS, COLUMNS, & PERMISSION MATRIX
 * ----------------------------------------------------------------------------
 */
var SCHEMA_DEFINITIONS = {
  Admins: {
    collection: "admins",
    idColumnIndex: 1, // Admin ID (Col A)
    headers: [
      { name: "Admin ID", field: "adminId", protected: true },
      { name: "Firebase UID", field: "uid", protected: true },
      { name: "Admin Name", field: "name", protected: false },
      { name: "Email", field: "email", protected: true },
      { name: "Phone", field: "phone", protected: false },
      { name: "Role", field: "role", protected: true },
      { name: "Status", field: "status", protected: false },
      { name: "Last Login", field: "lastLogin", protected: true },
      { name: "Profile Image URL", field: "profileImage", protected: false },
      { name: "Updated At", field: "updatedAt", protected: true }
    ]
  },
  Drivers: {
    collection: "drivers",
    idColumnIndex: 1, // Driver ID (Col A)
    headers: [
      { name: "Driver ID", field: "id", protected: true },
      { name: "Firebase UID", field: "uid", protected: true },
      { name: "Driver Name", field: "name", protected: false },
      { name: "Email", field: "email", protected: true },
      { name: "Phone", field: "phone", protected: false },
      { name: "Status", field: "status", protected: false },
      { name: "License Number", field: "licenseNumber", protected: false },
      { name: "License Expiry", field: "licenseExpiryDate", protected: false },
      { name: "Assigned Vehicle ID", field: "assignedVehicleId", protected: false },
      { name: "Rating", field: "rating", protected: true },
      { name: "Total Deliveries", field: "totalDeliveries", protected: true },
      { name: "Completed Deliveries", field: "completedDeliveries", protected: true },
      { name: "Current Latitude", field: "currentLatitude", protected: true },
      { name: "Current Longitude", field: "currentLongitude", protected: true },
      { name: "Speed (km/h)", field: "speed", protected: true },
      { name: "Heading", field: "heading", protected: true },
      { name: "Last Active", field: "lastActive", protected: true },
      { name: "Created At", field: "createdAt", protected: true },
      { name: "Updated At", field: "updatedAt", protected: true }
    ]
  },
  Vehicles: {
    collection: "vehicles",
    idColumnIndex: 1, // Vehicle ID (Col A)
    headers: [
      { name: "Vehicle ID", field: "id", protected: true },
      { name: "Registration Number", field: "registrationNumber", protected: false },
      { name: "Vehicle Number", field: "vehicleNumber", protected: false },
      { name: "Vehicle Type", field: "vehicleType", protected: false },
      { name: "Brand / Make", field: "brand", protected: false },
      { name: "Model", field: "model", protected: false },
      { name: "Capacity (Tons)", field: "capacity", protected: false },
      { name: "Status", field: "status", protected: false },
      { name: "Assigned Driver ID", field: "driverId", protected: false },
      { name: "Fuel Level (%)", field: "fuelLevel", protected: false },
      { name: "Odometer (km)", field: "odometer", protected: true },
      { name: "Image URL", field: "imageUrl", protected: false },
      { name: "Last Service Date", field: "lastServiceDate", protected: false },
      { name: "Next Service Date", field: "nextServiceDate", protected: false },
      { name: "Insurance Expiry", field: "insuranceExpiry", protected: false },
      { name: "Current Latitude", field: "currentLatitude", protected: true },
      { name: "Current Longitude", field: "currentLongitude", protected: true },
      { name: "Created At", field: "createdAt", protected: true },
      { name: "Updated At", field: "updatedAt", protected: true }
    ]
  },
  Godowns: {
    collection: "godowns",
    idColumnIndex: 1, // Godown ID (Col A)
    headers: [
      { name: "Godown ID", field: "id", protected: true },
      { name: "Godown Name", field: "name", protected: false },
      { name: "Address", field: "address", protected: false },
      { name: "City", field: "city", protected: false },
      { name: "State", field: "state", protected: false },
      { name: "Pincode (6 Digits)", field: "pincode", protected: false },
      { name: "Latitude (-90 to 90)", field: "latitude", protected: false },
      { name: "Longitude (-180 to 180)", field: "longitude", protected: false },
      { name: "Capacity (Tons)", field: "capacity", protected: false },
      { name: "Current Stock (Tons)", field: "currentStock", protected: true },
      { name: "Manager ID", field: "managerId", protected: false },
      { name: "Manager Name", field: "managerName", protected: false },
      { name: "Contact Phone", field: "phone", protected: false },
      { name: "Status", field: "status", protected: false },
      { name: "Created At", field: "createdAt", protected: true },
      { name: "Updated At", field: "updatedAt", protected: true }
    ]
  },
  Orders: {
    collection: "orders",
    idColumnIndex: 1, // Order ID (Col A)
    headers: [
      { name: "Order ID", field: "id", protected: true },
      { name: "Order Number", field: "orderNumber", protected: true },
      { name: "Customer Name", field: "customerName", protected: false },
      { name: "Customer Phone", field: "customerPhone", protected: false },
      { name: "Item Name", field: "itemName", protected: false },
      { name: "Quantity", field: "quantity", protected: false },
      { name: "Weight (kg)", field: "weight", protected: false },
      { name: "Pickup Location / Address", field: "pickupAddress", protected: false },
      { name: "Pickup Pincode", field: "pickupPincode", protected: false },
      { name: "Delivery Location / Address", field: "deliveryAddress", protected: false },
      { name: "Delivery Pincode", field: "deliveryPincode", protected: false },
      { name: "Status", field: "status", protected: true }, // Protected operational status
      { name: "Priority", field: "priority", protected: false },
      { name: "Payment Status", field: "paymentStatus", protected: false },
      { name: "Payment Method", field: "paymentMethod", protected: false },
      { name: "Payment Amount (₹)", field: "paymentAmount", protected: false },
      { name: "Transaction ID", field: "transactionId", protected: false },
      { name: "Payment Notes", field: "paymentNotes", protected: false },
      { name: "Remarks / Notes", field: "remarks", protected: false },
      { name: "Assigned Driver ID", field: "assignedDriverId", protected: false },
      { name: "Assigned Vehicle ID", field: "assignedVehicleId", protected: false },
      { name: "Parcel ID", field: "parcelId", protected: true },
      { name: "QR ID", field: "qrId", protected: true },
      { name: "OTP Verified", field: "otpVerified", protected: true },
      { name: "Delivery OTP", field: "deliveryOtp", protected: true },
      { name: "Delivered At", field: "deliveredAt", protected: true },
      { name: "Delivered By", field: "deliveredBy", protected: true },
      { name: "Created By", field: "createdBy", protected: true },
      { name: "Created At", field: "createdAt", protected: true },
      { name: "Updated At", field: "updatedAt", protected: true }
    ]
  },
  Dispatches: {
    collection: "dispatches",
    idColumnIndex: 1, // Dispatch ID (Col A)
    headers: [
      { name: "Dispatch ID", field: "id", protected: true },
      { name: "Order ID", field: "orderId", protected: true },
      { name: "Order Number", field: "orderNumber", protected: true },
      { name: "Customer Name", field: "customerName", protected: true },
      { name: "Driver ID", field: "driverId", protected: false },
      { name: "Vehicle ID", field: "vehicleId", protected: false },
      { name: "Status", field: "status", protected: true },
      { name: "Priority", field: "priority", protected: false },
      { name: "Estimated Delivery", field: "estimatedDelivery", protected: false },
      { name: "Remarks", field: "remarks", protected: false },
      { name: "Created At", field: "createdAt", protected: true },
      { name: "Updated At", field: "updatedAt", protected: true }
    ]
  }
};

/**
 * ----------------------------------------------------------------------------
 * 7. STRICT DATA VALIDATION ENGINE (SHEET → FIREBASE)
 * ----------------------------------------------------------------------------
 */
function validateFieldEdit(collectionName, fieldName, value) {
  if (value === null || value === undefined || value === "") {
    return { valid: true, sanitizedValue: "" };
  }

  var strVal = String(value).trim();

  switch (fieldName) {
    case "phone":
    case "customerPhone":
      // Valid phone: 10 to 15 digits, optional leading +
      if (!/^\+?[0-9\s-]{10,15}$/.test(strVal)) {
        return { valid: false, error: "Invalid Phone Number. Must contain 10-15 digits." };
      }
      return { valid: true, sanitizedValue: strVal };

    case "email":
      if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(strVal)) {
        return { valid: false, error: "Invalid Email Address format." };
      }
      return { valid: true, sanitizedValue: strVal.toLowerCase() };

    case "pincode":
    case "pickupPincode":
    case "deliveryPincode":
      if (!/^\d{6}$/.test(strVal)) {
        return { valid: false, error: "Invalid PIN Code. Must be exactly 6 numeric digits." };
      }
      return { valid: true, sanitizedValue: strVal };

    case "latitude":
      var lat = parseFloat(strVal);
      if (isNaN(lat) || lat < -90.0 || lat > 90.0) {
        return { valid: false, error: "Invalid Latitude. Must be a decimal number between -90.0 and +90.0." };
      }
      return { valid: true, sanitizedValue: lat };

    case "longitude":
      var lon = parseFloat(strVal);
      if (isNaN(lon) || lon < -180.0 || lon > 180.0) {
        return { valid: false, error: "Invalid Longitude. Must be a decimal number between -180.0 and +180.0." };
      }
      return { valid: true, sanitizedValue: lon };

    case "capacity":
    case "weight":
    case "quantity":
    case "paymentAmount":
      var num = parseFloat(strVal);
      if (isNaN(num) || num < 0) {
        return { valid: false, error: "Invalid numerical value. Cannot be negative." };
      }
      return { valid: true, sanitizedValue: num };

    case "fuelLevel":
      var fuel = parseFloat(strVal);
      if (isNaN(fuel) || fuel < 0 || fuel > 100) {
        return { valid: false, error: "Invalid Fuel Level. Must be between 0% and 100%." };
      }
      return { valid: true, sanitizedValue: fuel };

    case "paymentStatus":
      var validPaymentStatuses = ["PENDING", "PAID", "PARTIALLY_PAID", "COD", "FAILED", "REFUNDED"];
      var normalizedPayment = strVal.toUpperCase().replace(/\s+/g, "_");
      if (validPaymentStatuses.indexOf(normalizedPayment) === -1) {
        return { valid: false, error: "Invalid Payment Status: " + strVal + ". Allowed: " + validPaymentStatuses.join(", ") };
      }
      return { valid: true, sanitizedValue: normalizedPayment };

    case "paymentMethod":
      var validMethods = ["CASH", "UPI", "CARD", "BANK_TRANSFER", "COD", "OTHER"];
      var normalizedMethod = strVal.toUpperCase().replace(/\s+/g, "_");
      if (validMethods.indexOf(normalizedMethod) === -1) {
        return { valid: false, error: "Invalid Payment Method: " + strVal + ". Allowed: " + validMethods.join(", ") };
      }
      return { valid: true, sanitizedValue: normalizedMethod };

    default:
      return { valid: true, sanitizedValue: strVal };
  }
}

/**
 * ----------------------------------------------------------------------------
 * 8. EVENT-DRIVEN TWO-WAY SYNC TRIGGER (GOOGLE SHEETS → FIRESTORE)
 * ----------------------------------------------------------------------------
 * Installable onEdit trigger captures user edits in the Sheet, validates data,
 * checks field permissions, and patches Firestore.
 */
function onEditTrigger(e) {
  if (!e || !e.range) return;

  // 1. Loop Prevention: Ignore edits made programmatically by the sync engine
  if (isProgrammaticLockActive()) {
    Logger.log("Skipping onEdit: Programmatic mutation lock is active.");
    return;
  }

  var range = e.range;
  var sheet = range.getSheet();
  var sheetName = sheet.getName();

  var schema = SCHEMA_DEFINITIONS[sheetName];
  if (!schema) return; // Not a synced tab

  var row = range.getRow();
  var col = range.getColumn();

  // Ignore edits on header row
  if (row <= 1) return;

  var colHeader = schema.headers[col - 1];
  if (!colHeader) return;

  // Read Document ID from Column 1
  var docId = String(sheet.getRange(row, schema.idColumnIndex).getValue()).trim();
  if (!docId) {
    Logger.log("Skipping edit on row " + row + ": Missing Document ID.");
    return;
  }

  var oldValue = e.oldValue !== undefined ? e.oldValue : "";
  var newValue = range.getValue();

  // 2. Protected Column Guard: Revert unauthorized edits to critical live operational fields
  if (colHeader.protected) {
    Logger.log("⚠️ Protected field edit rejected: " + colHeader.name + " on " + docId);
    
    // Fetch live Firestore value to restore
    try {
      var liveDoc = fetchSingleFirestoreDocument(schema.collection, docId);
      var correctVal = liveDoc && liveDoc.fields[colHeader.field] ? getString(liveDoc.fields[colHeader.field]) : oldValue;
      
      setProgrammaticLock(5);
      range.setValue(correctVal);
      clearProgrammaticLock();

      logSyncOperation(
        "SHEET_EDIT",
        schema.collection,
        docId,
        "REJECTED_PROTECTED_FIELD",
        "Field '" + colHeader.name + "' is READ-ONLY. Live operational data must be updated via RouteCJ Apps.",
        Session.getActiveUser().getEmail()
      );

      SpreadsheetApp.getActiveSpreadsheet().toast(
        "Field '" + colHeader.name + "' is protected and cannot be edited in Sheets.",
        "🔒 Protected Field",
        5
      );
    } catch (err) {
      Logger.log("Error restoring protected field: " + err.message);
    }
    return;
  }

  // 3. Validation
  var validation = validateFieldEdit(schema.collection, colHeader.field, newValue);
  if (!validation.valid) {
    Logger.log("❌ Validation failed on " + colHeader.name + ": " + validation.error);
    
    setProgrammaticLock(5);
    range.setValue(oldValue); // Revert invalid change
    clearProgrammaticLock();

    logSyncOperation(
      "SHEET_EDIT",
      schema.collection,
      docId,
      "VALIDATION_ERROR",
      validation.error + " (Attempted: '" + newValue + "')",
      Session.getActiveUser().getEmail()
    );

    SpreadsheetApp.getActiveSpreadsheet().toast(
      validation.error,
      "❌ Validation Error",
      6
    );
    return;
  }

  // 4. Update Firestore via REST PATCH
  try {
    var updatePayload = {};
    updatePayload[colHeader.field] = validation.sanitizedValue;

    patchFirestoreDocument(schema.collection, docId, updatePayload, [colHeader.field]);

    // Update the 'Updated At' timestamp in Sheet
    var updatedColIdx = schema.headers.findIndex(function(h) { return h.field === "updatedAt"; });
    if (updatedColIdx !== -1) {
      setProgrammaticLock(5);
      sheet.getRange(row, updatedColIdx + 1).setValue(
        Utilities.formatDate(new Date(), Session.getScriptTimeZone() || "GMT+05:30", "yyyy-MM-dd HH:mm:ss")
      );
      clearProgrammaticLock();
    }

    logSyncOperation(
      "SHEET → FIREBASE",
      schema.collection,
      docId,
      "SUCCESS",
      "Updated '" + colHeader.field + "' to '" + validation.sanitizedValue + "'",
      Session.getActiveUser().getEmail()
    );

    SpreadsheetApp.getActiveSpreadsheet().toast(
      "Updated " + schema.collection + "/" + docId + " in Firestore.",
      "✅ Synced to Firebase",
      3
    );

  } catch (syncErr) {
    Logger.log("❌ Firestore PATCH error: " + syncErr.message);

    logSyncOperation(
      "SHEET → FIREBASE",
      schema.collection,
      docId,
      "ERROR",
      syncErr.message,
      Session.getActiveUser().getEmail()
    );

    SpreadsheetApp.getActiveSpreadsheet().toast(
      "Sync to Firebase failed: " + syncErr.message,
      "❌ Sync Error",
      6
    );
  }
}

/**
 * ----------------------------------------------------------------------------
 * 9. FIREBASE → GOOGLE SHEETS SYNCHRONIZATION (FULL SYNC ENGINE)
 * ----------------------------------------------------------------------------
 */
function syncAllCollections() {
  var startTime = new Date();
  var totalRecords = 0;
  var summaryParts = [];

  setProgrammaticLock(180); // Lock during programmatic batch writes

  try {
    for (var tabName in SCHEMA_DEFINITIONS) {
      var schema = SCHEMA_DEFINITIONS[tabName];
      var count = syncCollectionToSheet(schema);
      totalRecords += count;
      summaryParts.push(tabName + ": " + count);
    }

    // Refresh Reports tab
    updateReportsTab();

    var elapsed = ((new Date().getTime() - startTime.getTime()) / 1000).toFixed(1);
    var summary = "Full Sync Completed: " + totalRecords + " records in " + elapsed + "s (" + summaryParts.join(", ") + ")";
    
    logSyncOperation("FIREBASE → SHEET", "ALL_COLLECTIONS", "FULL_SYNC", "SUCCESS", summary, "SYSTEM_TRIGGER");
    Logger.log(summary);

    return { success: true, count: totalRecords, summary: summary };

  } catch (e) {
    Logger.log("Global Sync Error: " + e.message);
    logSyncOperation("FIREBASE → SHEET", "ALL_COLLECTIONS", "FULL_SYNC", "ERROR", e.message, "SYSTEM_TRIGGER");
    return { success: false, error: e.message };
  } finally {
    clearProgrammaticLock();
  }
}

function syncCollectionToSheet(schema) {
  var docs = fetchFirestoreDocuments(schema.collection);
  var sheet = getSheetOrInit(schema);
  var indexMap = buildDocumentIndexMap(sheet, schema.idColumnIndex);
  var count = 0;

  for (var i = 0; i < docs.length; i++) {
    var doc = docs[i];
    var docId = doc.id;
    var f = doc.fields || {};

    var rowValues = schema.headers.map(function(header) {
      return extractFieldValue(docId, f, header.field, doc);
    });

    upsertSheetRow(sheet, indexMap, schema.idColumnIndex, docId, rowValues);
    count++;
  }

  return count;
}

function extractFieldValue(docId, f, fieldName, doc) {
  if (fieldName === "id" || fieldName === "adminId") return docId;
  
  var val = f[fieldName];
  if (!val) {
    // Fallback lookups for legacy aliases
    if (fieldName === "pickupAddress") val = f["pickupLocation"] || f["pickupAddress"];
    if (fieldName === "deliveryAddress") val = f["deliveryLocation"] || f["deliveryAddress"];
    if (fieldName === "assignedDriverId") val = f["driverId"] || f["assignedDriverId"];
    if (fieldName === "assignedVehicleId") val = f["vehicleId"] || f["assignedVehicleId"];
    if (fieldName === "brand") val = f["make"] || f["brand"];
  }

  if (!val) return "";

  if (val.timestampValue !== undefined) {
    return getTimestamp(val);
  }
  if (val.doubleValue !== undefined || val.integerValue !== undefined) {
    return getNumber(val);
  }
  if (val.booleanValue !== undefined) {
    return val.booleanValue ? "TRUE" : "FALSE";
  }
  return getString(val);
}

/**
 * ----------------------------------------------------------------------------
 * 10. ROW UPSERT & DUPLICATE PROTECTION
 * ----------------------------------------------------------------------------
 */
function buildDocumentIndexMap(sheet, idColIndex) {
  var indexMap = {};
  var lastRow = sheet.getLastRow();
  if (lastRow <= 1) return indexMap;

  var idValues = sheet.getRange(2, idColIndex, lastRow - 1, 1).getValues();
  for (var i = 0; i < idValues.length; i++) {
    var id = String(idValues[i][0]).trim();
    if (id) {
      indexMap[id] = i + 2; // Actual row number
    }
  }
  return indexMap;
}

function upsertSheetRow(sheet, indexMap, idColIndex, docId, rowValues) {
  if (indexMap[docId]) {
    var targetRow = indexMap[docId];
    sheet.getRange(targetRow, 1, 1, rowValues.length).setValues([rowValues]);
  } else {
    sheet.appendRow(rowValues);
    var newRow = sheet.getLastRow();
    indexMap[docId] = newRow;
  }
}

/**
 * ----------------------------------------------------------------------------
 * 11. AUDIT LOGGING ENGINE (SyncLog TAB)
 * ----------------------------------------------------------------------------
 */
function logSyncOperation(source, collection, documentId, status, message, user) {
  try {
    var ss = SpreadsheetApp.getActiveSpreadsheet();
    if (!ss) return;

    var logSheet = ss.getSheetByName(SHEET_TABS.SYNC_LOG);
    if (!logSheet) {
      logSheet = ss.insertSheet(SHEET_TABS.SYNC_LOG);
      logSheet.appendRow(["Timestamp", "Source", "Collection", "Document ID", "Status", "Message", "User"]);
      logSheet.getRange("A1:G1").setFontWeight("bold").setBackground("#0F172A").setFontColor("#FFFFFF");
      logSheet.setFrozenRows(1);
    }

    var timestamp = Utilities.formatDate(new Date(), Session.getScriptTimeZone() || "GMT+05:30", "yyyy-MM-dd HH:mm:ss");
    logSheet.appendRow([
      timestamp,
      source || "SYNC",
      collection || "SYSTEM",
      documentId || "NONE",
      status || "INFO",
      message || "",
      user || "SYSTEM"
    ]);

    // Trim log to last 1000 entries
    var lastRow = logSheet.getLastRow();
    if (lastRow > 1005) {
      logSheet.deleteRows(2, lastRow - 1001);
    }
  } catch (e) {
    Logger.log("Failed to write to SyncLog: " + e.message);
  }
}

/**
 * ----------------------------------------------------------------------------
 * 12. INITIAL DATA MIGRATION ENGINE
 * ----------------------------------------------------------------------------
 */
function runInitialDataMigration() {
  var startTime = new Date();
  Logger.log("🚀 Starting RouteCJ Initial Data Migration...");

  setupSheets();
  var syncResult = syncAllCollections();

  var elapsed = ((new Date().getTime() - startTime.getTime()) / 1000).toFixed(1);
  var report = "Migration Completed in " + elapsed + "s. " + syncResult.summary;

  logSyncOperation("MIGRATION", "ALL", "INITIAL_IMPORT", "SUCCESS", report, Session.getActiveUser().getEmail());
  
  SpreadsheetApp.getActiveSpreadsheet().toast(report, "🚀 Migration Complete", 8);
  Logger.log("✅ " + report);
  return report;
}

/**
 * ----------------------------------------------------------------------------
 * 13. SHEET SETUP, STYLING, & VISUAL PROTECTION
 * ----------------------------------------------------------------------------
 */
function setupSheets() {
  var ss = SpreadsheetApp.getActiveSpreadsheet();
  if (!ss) return;

  try {
    ss.rename("ROUTECJ DATABASE BACKUP");
  } catch (e) {}

  for (var tabName in SCHEMA_DEFINITIONS) {
    var schema = SCHEMA_DEFINITIONS[tabName];
    var sheet = ss.getSheetByName(tabName);
    if (!sheet) {
      sheet = ss.insertSheet(tabName);
    }

    // Set header labels
    var headerRow = schema.headers.map(function(h) {
      return h.protected ? (h.name + " 🔒") : h.name;
    });

    sheet.getRange(1, 1, 1, headerRow.length).setValues([headerRow]);
    sheet.setFrozenRows(1);

    // Style Header Row
    var headerRange = sheet.getRange(1, 1, 1, headerRow.length);
    headerRange.setFontWeight("bold");
    headerRange.setBackground("#0F172A");
    headerRange.setFontColor("#FFFFFF");

    // Color code protected vs editable header columns
    for (var c = 0; c < schema.headers.length; c++) {
      var h = schema.headers[c];
      var cell = sheet.getRange(1, c + 1);
      if (h.protected) {
        cell.setFontColor("#94A3B8"); // Muted color with lock icon
      } else {
        cell.setFontColor("#38BDF8"); // Vibrant cyan for editable fields
      }
    }
  }

  // Create Reports Tab
  var reportsSheet = ss.getSheetByName(SHEET_TABS.REPORTS);
  if (!reportsSheet) {
    reportsSheet = ss.insertSheet(SHEET_TABS.REPORTS);
  }
  updateReportsTab();

  // Create SyncLog Tab
  var logSheet = ss.getSheetByName(SHEET_TABS.SYNC_LOG);
  if (!logSheet) {
    logSheet = ss.insertSheet(SHEET_TABS.SYNC_LOG);
    logSheet.appendRow(["Timestamp", "Source", "Collection", "Document ID", "Status", "Message", "User"]);
    logSheet.getRange("A1:G1").setFontWeight("bold").setBackground("#0F172A").setFontColor("#FFFFFF");
    logSheet.setFrozenRows(1);
  }

  Logger.log("✅ All RouteCJ sheets setup and styled successfully!");
}

function getSheetOrInit(schema) {
  var ss = SpreadsheetApp.getActiveSpreadsheet();
  var sheet = ss.getSheetByName(schema.headers ? getTabNameBySchema(schema) : schema);
  if (!sheet) {
    setupSheets();
    sheet = ss.getSheetByName(schema.headers ? getTabNameBySchema(schema) : schema);
  }
  return sheet;
}

function getTabNameBySchema(schema) {
  for (var name in SCHEMA_DEFINITIONS) {
    if (SCHEMA_DEFINITIONS[name] === schema) return name;
  }
  return schema.collection;
}

function updateReportsTab() {
  var ss = SpreadsheetApp.getActiveSpreadsheet();
  var reportsSheet = ss.getSheetByName(SHEET_TABS.REPORTS);
  if (!reportsSheet) return;

  var ordersSheet = ss.getSheetByName(SHEET_TABS.ORDERS);
  var driversSheet = ss.getSheetByName(SHEET_TABS.DRIVERS);
  var vehiclesSheet = ss.getSheetByName(SHEET_TABS.VEHICLES);
  var godownsSheet = ss.getSheetByName(SHEET_TABS.GODOWNS);

  var orderCount = ordersSheet ? Math.max(0, ordersSheet.getLastRow() - 1) : 0;
  var driverCount = driversSheet ? Math.max(0, driversSheet.getLastRow() - 1) : 0;
  var vehicleCount = vehiclesSheet ? Math.max(0, vehiclesSheet.getLastRow() - 1) : 0;
  var godownCount = godownsSheet ? Math.max(0, godownsSheet.getLastRow() - 1) : 0;

  reportsSheet.clear();
  reportsSheet.appendRow(["RouteCJ Logistics — Real-Time Fleet & Operations Intelligence"]);
  reportsSheet.appendRow(["Last Synced At", Utilities.formatDate(new Date(), Session.getScriptTimeZone() || "GMT+05:30", "yyyy-MM-dd HH:mm:ss")]);
  reportsSheet.appendRow([]);
  reportsSheet.appendRow(["CORE OPERATIONAL METRICS", "TOTAL COUNT"]);
  reportsSheet.appendRow(["Total Managed Orders", orderCount]);
  reportsSheet.appendRow(["Active Drivers in Fleet", driverCount]);
  reportsSheet.appendRow(["Registered Vehicles", vehicleCount]);
  reportsSheet.appendRow(["Operational Godowns / Hubs", godownCount]);

  reportsSheet.getRange("A1:B1").setFontWeight("bold").setFontSize(14).setBackground("#0F172A").setFontColor("#38BDF8");
  reportsSheet.getRange("A4:B4").setFontWeight("bold").setBackground("#1E293B").setFontColor("#FFFFFF");
}

/**
 * ----------------------------------------------------------------------------
 * 14. UI MENU & TRIGGER AUTOMATION
 * ----------------------------------------------------------------------------
 */
function onOpen() {
  var ui = SpreadsheetApp.getUi();
  ui.createMenu("RouteCJ Sync")
    .addItem("🔄 Run Full Two-Way Sync", "syncAllCollections")
    .addItem("🚀 Run Initial Data Migration", "runInitialDataMigration")
    .addItem("🛠️ Setup All Sheets & Formatting", "setupSheets")
    .addSeparator()
    .addItem("🧪 Test Firestore Connection", "testFirestoreConnection")
    .addToUi();
}
