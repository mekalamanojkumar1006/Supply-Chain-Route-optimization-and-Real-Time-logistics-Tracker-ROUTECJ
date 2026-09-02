/**
 * ============================================================================
 * ROUTECJ LOGISTICS ECOSYSTEM — MASTER FIREBASE ↔ GOOGLE SHEETS SYNC ENGINE
 * ============================================================================
 *
 * Master Spreadsheet ID: 17nwnNtKfBcw8zr2elXcAn3mbLS-PtLrSj9vIi4RJMLw
 * Project ID: supplychaintracking-21492
 *
 * Architecture:
 *   RouteCJ Mobile/Web Apps (Admin, Driver, Customer)
 *          ↕ (Live Operations: GPS, QR, Dispatch, Delivery OTP, Auth)
 *   Firebase Firestore (PRIMARY DATABASE & SOURCE OF TRUTH)
 *          ↕ (Automatic Two-Way Synchronization Layer)
 *   Google Apps Script Sync Service (OAuth 2.0 JWT Bearer Token Authentication)
 *          ↕ (Controlled Administrative Management Mirror)
 *   Google Spreadsheet (Orders, Dispatches, Drivers, Vehicles, Godowns, Admins, Tracking, Backup Log)
 *
 * Guarantees:
 *   1. Zero private keys stored in Android app, Git repo, or Google Sheet cells.
 *   2. Strict validation & loop prevention (ROUTECJ_PROGRAMMATIC_MUTATION_LOCK).
 *   3. 100% Header and column alignment with the master Google Spreadsheet.
 *   4. Multi-alias parser supporting camelCase, snake_case, and legacy field names.
 *   5. Robust Last Login synchronization from Firestore server timestamps.
 *   6. Deduplication by canonical Document ID, UID, and business ID (e.g. ADMIN001).
 *   7. Dedicated Empty Field Audit diagnostic tool.
 * ============================================================================
 */

// SPREADSHEET TAB DEFINITIONS (EXACT MATCH TO MASTER SPREADSHEET)
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

// CACHE KEYS & CONFIGURATION
var CACHE_KEYS = {
  OAUTH_TOKEN: "FIRESTORE_OAUTH_TOKEN",
  MUTATION_LOCK: "ROUTECJ_PROGRAMMATIC_MUTATION_LOCK"
};

/**
 * ----------------------------------------------------------------------------
 * 1. SCRIPT CONFIGURATION & SCRIPT PROPERTIES
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

    Logger.log("2. Testing Firestore REST read on 'admins' collection...");
    var docs = fetchFirestoreDocuments("admins");
    Logger.log("✅ Successfully connected! Found " + docs.length + " documents in 'admins'.");

    Logger.log("3. Testing Firestore REST read on 'orders' collection...");
    var orderDocs = fetchFirestoreDocuments("orders");
    Logger.log("✅ Successfully connected! Found " + orderDocs.length + " documents in 'orders'.");
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

  // Attach sync metadata
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
 * 4. FIELD CONVERSIONS & ROBUST DATA PARSERS
 * ----------------------------------------------------------------------------
 */
function getString(f) {
  if (!f) return "";
  if (f.stringValue !== undefined) return f.stringValue;
  if (f.integerValue !== undefined) return String(f.integerValue);
  if (f.doubleValue !== undefined) return String(f.doubleValue);
  if (f.booleanValue !== undefined) return f.booleanValue ? "TRUE" : "FALSE";
  if (f.timestampValue !== undefined) return getTimestamp(f);
  if (f.referenceValue !== undefined) return f.referenceValue;
  return "";
}

function getNumber(f) {
  if (!f) return 0;
  if (f.integerValue !== undefined) return parseInt(f.integerValue, 10);
  if (f.doubleValue !== undefined) return parseFloat(f.doubleValue);
  if (f.stringValue !== undefined) {
    var num = parseFloat(f.stringValue);
    return isNaN(num) ? 0 : num;
  }
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
  var raw = f.timestampValue || f.stringValue;
  if (f.integerValue !== undefined) {
    var millis = parseInt(f.integerValue, 10);
    try {
      var d = new Date(millis);
      return Utilities.formatDate(d, Session.getScriptTimeZone() || "GMT+05:30", "yyyy-MM-dd HH:mm:ss");
    } catch (e) {}
  }
  if (raw) {
    try {
      var d = new Date(raw);
      if (!isNaN(d.getTime())) {
        return Utilities.formatDate(d, Session.getScriptTimeZone() || "GMT+05:30", "yyyy-MM-dd HH:mm:ss");
      }
    } catch (e) {}
    return String(raw);
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
 * 6. SHEET DEFINITIONS & COLUMN SCHEMAS (MATCHES MASTER SPREADSHEET 100%)
 * ----------------------------------------------------------------------------
 */
var SCHEMA_DEFINITIONS = {
  Orders: {
    collection: "orders",
    idColumnIndex: 1, // Order ID (Col A)
    headers: [
      { name: "Order ID", field: "id", aliases: ["id", "orderId", "_id"], type: "string", protected: true },
      { name: "Order Number", field: "orderNumber", aliases: ["orderNumber", "order_number"], type: "string", protected: true },
      { name: "Customer Name", field: "customerName", aliases: ["customerName", "customer_name"], type: "string", protected: false },
      { name: "Customer Phone", field: "customerPhone", aliases: ["customerPhone", "customer_phone", "phone"], type: "string", protected: false },
      { name: "Item Name", field: "itemName", aliases: ["itemName", "item_name"], type: "string", protected: false },
      { name: "Quantity", field: "quantity", aliases: ["quantity", "qty"], type: "number", protected: false },
      { name: "Weight (kg/tons)", field: "weight", aliases: ["weight", "weightKg", "weight_kg"], type: "number", protected: false },
      { name: "Pickup Location", field: "pickupLocation", aliases: ["pickupLocation", "pickupAddress", "pickup_location", "pickup_address"], type: "string", protected: false },
      { name: "Pickup Pincode", field: "pickupPincode", aliases: ["pickupPincode", "pickup_pincode"], type: "string", protected: false },
      { name: "Delivery Location", field: "deliveryLocation", aliases: ["deliveryLocation", "deliveryAddress", "delivery_location", "delivery_address", "customerAddress"], type: "string", protected: false },
      { name: "Delivery Pincode", field: "deliveryPincode", aliases: ["deliveryPincode", "delivery_pincode"], type: "string", protected: false },
      { name: "Status", field: "status", aliases: ["status", "orderStatus", "order_status"], type: "string", protected: true },
      { name: "Priority", field: "priority", aliases: ["priority"], type: "string", protected: false },
      { name: "Payment Status", field: "paymentStatus", aliases: ["paymentStatus", "payment_status"], type: "string", protected: false },
      { name: "Total Amount", field: "totalAmount", aliases: ["totalAmount", "paymentAmount", "total_amount", "payment_amount", "amount"], type: "number", protected: false },
      { name: "Driver ID", field: "driverId", aliases: ["driverId", "assignedDriverId", "driver_id", "assigned_driver_id"], type: "string", protected: false },
      { name: "Driver Name", field: "driverName", aliases: ["driverName", "driver_name", "assignedDriverName"], type: "string", protected: true },
      { name: "Vehicle ID", field: "vehicleId", aliases: ["vehicleId", "assignedVehicleId", "vehicle_id", "assigned_vehicle_id"], type: "string", protected: false },
      { name: "Vehicle Registration", field: "vehicleRegistration", aliases: ["vehicleRegistration", "vehicle_registration", "registrationNumber", "vehicleNumber"], type: "string", protected: true },
      { name: "Parcel ID", field: "parcelId", aliases: ["parcelId", "parcel_id"], type: "string", protected: true },
      { name: "QR ID", field: "qrId", aliases: ["qrId", "qr_id"], type: "string", protected: true },
      { name: "OTP Verified", field: "otpVerified", aliases: ["otpVerified", "otp_verified"], type: "boolean", protected: true },
      { name: "Delivered At", field: "deliveredAt", aliases: ["deliveredAt", "delivered_at"], type: "timestamp", protected: true },
      { name: "Delivered By", field: "deliveredBy", aliases: ["deliveredBy", "delivered_by"], type: "string", protected: true },
      { name: "Delivery Remarks", field: "deliveryRemarks", aliases: ["deliveryRemarks", "delivery_remarks", "remarks", "notes"], type: "string", protected: false },
      { name: "Created By", field: "createdBy", aliases: ["createdBy", "created_by"], type: "string", protected: true },
      { name: "Created By Role", field: "createdByRole", aliases: ["createdByRole", "created_by_role"], type: "string", protected: true },
      { name: "Source", field: "source", aliases: ["source"], type: "string", protected: true },
      { name: "Created At", field: "createdAt", aliases: ["createdAt", "created_at"], type: "timestamp", protected: true },
      { name: "Updated At", field: "updatedAt", aliases: ["updatedAt", "updated_at"], type: "timestamp", protected: true }
    ]
  },
  Dispatches: {
    collection: "dispatches",
    idColumnIndex: 1, // Dispatch ID (Col A)
    headers: [
      { name: "Dispatch ID", field: "id", aliases: ["id", "dispatchId", "_id"], type: "string", protected: true },
      { name: "Order ID", field: "orderId", aliases: ["orderId", "order_id"], type: "string", protected: true },
      { name: "Order Number", field: "orderNumber", aliases: ["orderNumber", "order_number"], type: "string", protected: true },
      { name: "Customer Name", field: "customerName", aliases: ["customerName", "customer_name"], type: "string", protected: true },
      { name: "Driver ID", field: "driverId", aliases: ["driverId", "driver_id", "assignedDriverId"], type: "string", protected: false },
      { name: "Driver Name", field: "driverName", aliases: ["driverName", "driver_name"], type: "string", protected: true },
      { name: "Vehicle ID", field: "vehicleId", aliases: ["vehicleId", "vehicle_id", "assignedVehicleId"], type: "string", protected: false },
      { name: "Vehicle Registration", field: "vehicleRegistration", aliases: ["vehicleRegistration", "vehicle_registration", "registrationNumber"], type: "string", protected: true },
      { name: "Pickup Location", field: "pickupLocation", aliases: ["pickupLocation", "pickupAddress", "pickup_location"], type: "string", protected: true },
      { name: "Delivery Location", field: "deliveryLocation", aliases: ["deliveryLocation", "deliveryAddress", "delivery_location"], type: "string", protected: true },
      { name: "Status", field: "status", aliases: ["status", "dispatchStatus"], type: "string", protected: true },
      { name: "Priority", field: "priority", aliases: ["priority"], type: "string", protected: false },
      { name: "Estimated Delivery", field: "estimatedDelivery", aliases: ["estimatedDelivery", "estimated_delivery", "estimatedDeliveryDate"], type: "timestamp", protected: false },
      { name: "Remarks", field: "remarks", aliases: ["remarks", "notes"], type: "string", protected: false },
      { name: "Created At", field: "createdAt", aliases: ["createdAt", "created_at"], type: "timestamp", protected: true },
      { name: "Updated At", field: "updatedAt", aliases: ["updatedAt", "updated_at"], type: "timestamp", protected: true }
    ]
  },
  Drivers: {
    collection: "drivers",
    idColumnIndex: 1, // Driver ID (Col A)
    headers: [
      { name: "Driver ID", field: "id", aliases: ["id", "driverId", "_id"], type: "string", protected: true },
      { name: "Firebase UID", field: "uid", aliases: ["uid", "userId"], type: "string", protected: true },
      { name: "Driver Name", field: "name", aliases: ["name", "driverName", "driver_name"], type: "string", protected: false },
      { name: "Email", field: "email", aliases: ["email"], type: "string", protected: true },
      { name: "Phone", field: "phone", aliases: ["phone", "phoneNumber", "phone_number"], type: "string", protected: false },
      { name: "Status", field: "status", aliases: ["status", "driverStatus"], type: "string", protected: false },
      { name: "License Number", field: "licenseNumber", aliases: ["licenseNumber", "license_number"], type: "string", protected: false },
      { name: "License Expiry", field: "licenseExpiryDate", aliases: ["licenseExpiryDate", "license_expiry_date", "licenseExpiry"], type: "timestamp", protected: false },
      { name: "Assigned Vehicle", field: "assignedVehicle", aliases: ["assignedVehicle", "assigned_vehicle", "assignedVehicleId", "assigned_vehicle_id", "vehicleId"], type: "string", protected: false },
      { name: "Rating", field: "rating", aliases: ["rating"], type: "number", protected: true },
      { name: "Total Deliveries", field: "totalDeliveries", aliases: ["totalDeliveries", "total_deliveries"], type: "number", protected: true },
      { name: "Completed Deliveries", field: "completedDeliveries", aliases: ["completedDeliveries", "completed_deliveries"], type: "number", protected: true },
      { name: "Current Latitude", field: "currentLatitude", aliases: ["currentLatitude", "current_latitude", "latitude", "lat"], type: "number", protected: true },
      { name: "Current Longitude", field: "currentLongitude", aliases: ["currentLongitude", "current_longitude", "longitude", "lng", "lon"], type: "number", protected: true },
      { name: "Speed (km/h)", field: "speed", aliases: ["speed"], type: "number", protected: true },
      { name: "Heading", field: "heading", aliases: ["heading"], type: "number", protected: true },
      { name: "Last Active", field: "lastActive", aliases: ["lastActive", "last_active"], type: "timestamp", protected: true },
      { name: "Created At", field: "createdAt", aliases: ["createdAt", "created_at", "joinedDate"], type: "timestamp", protected: true }
    ]
  },
  Vehicles: {
    collection: "vehicles",
    idColumnIndex: 1, // Vehicle ID (Col A)
    headers: [
      { name: "Vehicle ID", field: "id", aliases: ["id", "vehicleId", "_id"], type: "string", protected: true },
      { name: "Registration Number", field: "registrationNumber", aliases: ["registrationNumber", "registration_number", "vehicleNumber", "vehicle_number"], type: "string", protected: false },
      { name: "Vehicle Type", field: "vehicleType", aliases: ["vehicleType", "vehicle_type", "type"], type: "string", protected: false },
      { name: "Make / Brand", field: "brand", aliases: ["brand", "make", "brandMake", "make_brand"], type: "string", protected: false },
      { name: "Model", field: "model", aliases: ["model"], type: "string", protected: false },
      { name: "Capacity", field: "capacity", aliases: ["capacity", "capacityTons"], type: "number", protected: false },
      { name: "Capacity Unit", field: "capacityUnit", aliases: ["capacityUnit", "capacity_unit", "unit"], type: "string", protected: false },
      { name: "Status", field: "status", aliases: ["status", "vehicleStatus"], type: "string", protected: false },
      { name: "Assigned Driver ID", field: "driverId", aliases: ["driverId", "assignedDriverId", "driver_id"], type: "string", protected: false },
      { name: "Assigned Driver Name", field: "driverName", aliases: ["driverName", "assignedDriverName", "driver_name"], type: "string", protected: false },
      { name: "Fuel Level (%)", field: "fuelLevel", aliases: ["fuelLevel", "fuel_level"], type: "number", protected: false },
      { name: "Odometer (km)", field: "odometer", aliases: ["odometer", "mileage"], type: "number", protected: true },
      { name: "Image URL", field: "imageUrl", aliases: ["imageUrl", "image_url", "profileImage"], type: "string", protected: false },
      { name: "Last Service Date", field: "lastServiceDate", aliases: ["lastServiceDate", "last_service_date"], type: "timestamp", protected: false },
      { name: "Next Service Date", field: "nextServiceDate", aliases: ["nextServiceDate", "next_service_date"], type: "timestamp", protected: false },
      { name: "Insurance Expiry", field: "insuranceExpiry", aliases: ["insuranceExpiry", "insurance_expiry"], type: "timestamp", protected: false },
      { name: "Created At", field: "createdAt", aliases: ["createdAt", "created_at"], type: "timestamp", protected: true },
      { name: "Updated At", field: "updatedAt", aliases: ["updatedAt", "updated_at"], type: "timestamp", protected: true }
    ]
  },
  Godowns: {
    collection: "godowns",
    idColumnIndex: 1, // Godown ID (Col A)
    headers: [
      { name: "Godown ID", field: "id", aliases: ["id", "godownId", "_id"], type: "string", protected: true },
      { name: "Godown Name", field: "name", aliases: ["name", "godownName", "godown_name"], type: "string", protected: false },
      { name: "Address", field: "address", aliases: ["address"], type: "string", protected: false },
      { name: "City", field: "city", aliases: ["city"], type: "string", protected: false },
      { name: "State", field: "state", aliases: ["state"], type: "string", protected: false },
      { name: "Pincode", field: "pincode", aliases: ["pincode", "zipCode", "pin_code"], type: "string", protected: false },
      { name: "Latitude", field: "latitude", aliases: ["latitude", "lat"], type: "number", protected: false },
      { name: "Longitude", field: "longitude", aliases: ["longitude", "lng", "lon"], type: "number", protected: false },
      { name: "Capacity (Tons)", field: "capacity", aliases: ["capacity", "capacityTons"], type: "number", protected: false },
      { name: "Current Stock (Tons)", field: "currentStock", aliases: ["currentStock", "current_stock", "stock"], type: "number", protected: true },
      { name: "Manager ID", field: "managerId", aliases: ["managerId", "manager_id"], type: "string", protected: false },
      { name: "Manager Name", field: "managerName", aliases: ["managerName", "manager_name"], type: "string", protected: false },
      { name: "Contact Phone", field: "phone", aliases: ["phone", "contactPhone", "contact_phone"], type: "string", protected: false },
      { name: "Status", field: "status", aliases: ["status"], type: "string", protected: false },
      { name: "Created At", field: "createdAt", aliases: ["createdAt", "created_at"], type: "timestamp", protected: true },
      { name: "Updated At", field: "updatedAt", aliases: ["updatedAt", "updated_at"], type: "timestamp", protected: true }
    ]
  },
  Admins: {
    collection: "admins",
    idColumnIndex: 1, // Admin ID (Col A)
    headers: [
      { name: "Admin ID", field: "adminId", aliases: ["adminId", "admin_id", "id", "_id"], type: "string", protected: true },
      { name: "Firebase UID", field: "uid", aliases: ["uid", "userId"], type: "string", protected: true },
      { name: "Admin Name", field: "name", aliases: ["name", "adminName", "admin_name"], type: "string", protected: false },
      { name: "Email", field: "email", aliases: ["email"], type: "string", protected: true },
      { name: "Phone", field: "phone", aliases: ["phone", "phoneNumber"], type: "string", protected: false },
      { name: "Role", field: "role", aliases: ["role", "adminRole"], type: "string", protected: true },
      { name: "Status", field: "status", aliases: ["status"], type: "string", protected: false },
      { name: "Last Login", field: "lastLogin", aliases: ["lastLogin", "last_login", "lastSignInTime", "last_sign_in_time"], type: "timestamp", protected: true },
      { name: "Profile Image URL", field: "profileImage", aliases: ["profileImage", "profile_image", "imageUrl"], type: "string", protected: false }
    ]
  }
};

/**
 * ----------------------------------------------------------------------------
 * 7. FIELD VALUE EXTRACTOR (MULTI-ALIAS & CASING RESOLVER)
 * ----------------------------------------------------------------------------
 */
function extractFieldValue(docId, f, headerDef, doc) {
  var aliases = headerDef.aliases || [headerDef.field];
  var rawVal = null;

  for (var i = 0; i < aliases.length; i++) {
    var key = aliases[i];
    if (f[key] !== undefined && f[key] !== null) {
      rawVal = f[key];
      break;
    }
  }

  // Handle special ID fallbacks
  if (rawVal === null || rawVal === undefined) {
    if (headerDef.field === "id" || headerDef.field === "adminId") {
      return docId;
    }
    return "";
  }

  // Convert Firestore types
  if (headerDef.type === "timestamp" || rawVal.timestampValue !== undefined) {
    return getTimestamp(rawVal);
  }
  if (headerDef.type === "number" || rawVal.doubleValue !== undefined || rawVal.integerValue !== undefined) {
    var num = getNumber(rawVal);
    return isNaN(num) ? "" : num;
  }
  if (headerDef.type === "boolean" || rawVal.booleanValue !== undefined) {
    return getBoolean(rawVal);
  }
  return getString(rawVal);
}

/**
 * ----------------------------------------------------------------------------
 * 8. STRICT DATA VALIDATION ENGINE (SHEET → FIREBASE)
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
    case "totalAmount":
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

    default:
      return { valid: true, sanitizedValue: strVal };
  }
}

/**
 * ----------------------------------------------------------------------------
 * 9. EVENT-DRIVEN TWO-WAY SYNC TRIGGER (GOOGLE SHEETS → FIRESTORE)
 * ----------------------------------------------------------------------------
 */
function onEditTrigger(e) {
  if (!e || !e.range) return;

  // 1. Loop Prevention
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

  // 2. Protected Column Guard
  if (colHeader.protected) {
    Logger.log("⚠️ Protected field edit rejected: " + colHeader.name + " on " + docId);
    
    try {
      var liveDoc = fetchSingleFirestoreDocument(schema.collection, docId);
      var correctVal = liveDoc ? extractFieldValue(docId, liveDoc.fields, colHeader, liveDoc) : oldValue;
      
      setProgrammaticLock(5);
      range.setValue(correctVal);
      clearProgrammaticLock();

      logSyncOperation(
        schema.collection,
        docId,
        "SHEET_EDIT",
        "REJECTED",
        "Field '" + colHeader.name + "' is READ-ONLY. Live operational data is managed via RouteCJ Apps."
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
    range.setValue(oldValue); // Revert
    clearProgrammaticLock();

    logSyncOperation(
      schema.collection,
      docId,
      "VALIDATION",
      "REJECTED",
      validation.error + " (Attempted: '" + newValue + "')"
    );

    SpreadsheetApp.getActiveSpreadsheet().toast(validation.error, "❌ Validation Error", 6);
    return;
  }

  // 4. Update Firestore via REST PATCH
  try {
    var updatePayload = {};
    updatePayload[colHeader.field] = validation.sanitizedValue;

    patchFirestoreDocument(schema.collection, docId, updatePayload, [colHeader.field]);

    // Update the 'Updated At' timestamp in Sheet if present
    var updatedColIdx = schema.headers.findIndex(function(h) { return h.field === "updatedAt"; });
    if (updatedColIdx !== -1) {
      setProgrammaticLock(5);
      sheet.getRange(row, updatedColIdx + 1).setValue(
        Utilities.formatDate(new Date(), Session.getScriptTimeZone() || "GMT+05:30", "yyyy-MM-dd HH:mm:ss")
      );
      clearProgrammaticLock();
    }

    logSyncOperation(
      schema.collection,
      docId,
      "SHEET → FIREBASE",
      "SUCCESS",
      "Updated '" + colHeader.field + "' to '" + validation.sanitizedValue + "'"
    );

    SpreadsheetApp.getActiveSpreadsheet().toast("Updated " + schema.collection + "/" + docId + " in Firestore.", "✅ Synced to Firebase", 3);

  } catch (syncErr) {
    Logger.log("❌ Firestore PATCH error: " + syncErr.message);

    logSyncOperation(
      schema.collection,
      docId,
      "SHEET → FIREBASE",
      "ERROR",
      syncErr.message
    );

    SpreadsheetApp.getActiveSpreadsheet().toast("Sync to Firebase failed: " + syncErr.message, "❌ Sync Error", 6);
  }
}

/**
 * ----------------------------------------------------------------------------
 * 10. FIREBASE → GOOGLE SHEETS SYNCHRONIZATION ENGINE
 * ----------------------------------------------------------------------------
 */
function syncAllCollections() {
  var startTime = new Date();
  var totalRecords = 0;
  var summaryParts = [];

  setProgrammaticLock(180); // Lock during programmatic batch writes

  try {
    // 1. Sync standard mapped collections
    for (var tabName in SCHEMA_DEFINITIONS) {
      var schema = SCHEMA_DEFINITIONS[tabName];
      var count = syncCollectionToSheet(schema, tabName);
      totalRecords += count;
      summaryParts.push(tabName + ": " + count);
    }

    // 2. Refresh live Tracking tab
    var trackingCount = updateTrackingTab();
    summaryParts.push("Tracking: " + trackingCount);

    var elapsed = ((new Date().getTime() - startTime.getTime()) / 1000).toFixed(1);
    var summary = "Full Sync Completed: " + totalRecords + " total records in " + elapsed + "s (" + summaryParts.join(", ") + ")";
    
    logSyncOperation("ALL_COLLECTIONS", "FULL_SYNC", "FIREBASE → SHEET", "SUCCESS", summary);
    Logger.log("✅ " + summary);

    return { success: true, count: totalRecords, summary: summary };

  } catch (e) {
    Logger.log("Global Sync Error: " + e.message);
    logSyncOperation("ALL_COLLECTIONS", "FULL_SYNC", "FIREBASE → SHEET", "ERROR", e.message);
    return { success: false, error: e.message };
  } finally {
    clearProgrammaticLock();
  }
}

function syncCollectionToSheet(schema, tabName) {
  var docs = fetchFirestoreDocuments(schema.collection);
  var sheet = getSheetOrInit(tabName, schema);
  var indexMap = buildDocumentIndexMap(sheet, schema);
  var count = 0;

  // Deduplication map for in-memory merging (especially Admins by UID and adminId)
  var processedKeys = {};

  for (var i = 0; i < docs.length; i++) {
    var doc = docs[i];
    var docId = doc.id;
    var f = doc.fields || {};

    // Deduplication key
    var dedupKey = docId;
    if (tabName === SHEET_TABS.ADMINS) {
      var adminId = getString(f["adminId"]) || getString(f["id"]);
      var email = getString(f["email"]).toLowerCase();
      var uid = getString(f["uid"]);
      dedupKey = adminId || uid || email || docId;
      if (processedKeys[dedupKey]) {
        continue; // Skip duplicate admin entry
      }
      processedKeys[dedupKey] = true;
      if (adminId) processedKeys[adminId] = true;
      if (uid) processedKeys[uid] = true;
    }

    var rowValues = schema.headers.map(function(header) {
      return extractFieldValue(docId, f, header, doc);
    });

    upsertSheetRow(sheet, indexMap, schema, docId, dedupKey, rowValues);
    count++;
  }

  return count;
}

/**
 * ----------------------------------------------------------------------------
 * 11. ROW UPSERT & DEDUPLICATION ENGINE
 * ----------------------------------------------------------------------------
 */
function buildDocumentIndexMap(sheet, schema) {
  var indexMap = {};
  var lastRow = sheet.getLastRow();
  if (lastRow <= 1) return indexMap;

  var idColIndex = schema.idColumnIndex || 1;
  var idValues = sheet.getRange(2, idColIndex, lastRow - 1, 1).getValues();
  
  for (var i = 0; i < idValues.length; i++) {
    var id = String(idValues[i][0]).trim();
    if (id) {
      indexMap[id] = i + 2; // Actual row number
    }
  }

  // For Admins, also index by Firebase UID (Col B) if available
  if (schema.collection === "admins" && schema.headers.length >= 2) {
    var uidValues = sheet.getRange(2, 2, lastRow - 1, 1).getValues();
    for (var j = 0; j < uidValues.length; j++) {
      var uid = String(uidValues[j][0]).trim();
      if (uid && !indexMap[uid]) {
        indexMap[uid] = j + 2;
      }
    }
  }

  return indexMap;
}

function upsertSheetRow(sheet, indexMap, schema, docId, dedupKey, rowValues) {
  var targetRow = indexMap[docId] || indexMap[dedupKey];
  
  if (targetRow && targetRow <= sheet.getLastRow()) {
    sheet.getRange(targetRow, 1, 1, rowValues.length).setValues([rowValues]);
  } else {
    sheet.appendRow(rowValues);
    var newRow = sheet.getLastRow();
    indexMap[docId] = newRow;
    if (dedupKey) indexMap[dedupKey] = newRow;
  }
}

/**
 * ----------------------------------------------------------------------------
 * 12. LIVE TRACKING TAB INTELLIGENCE AGGREGATOR
 * ----------------------------------------------------------------------------
 */
function updateTrackingTab() {
  var ss = SpreadsheetApp.getActiveSpreadsheet();
  var trackingSheet = ss.getSheetByName(SHEET_TABS.TRACKING);
  if (!trackingSheet) {
    trackingSheet = ss.insertSheet(SHEET_TABS.TRACKING);
  }

  // Fetch all related collections
  var dispatches = fetchFirestoreDocuments("dispatches");
  var drivers = fetchFirestoreDocuments("drivers");
  var vehicles = fetchFirestoreDocuments("vehicles");
  var orders = fetchFirestoreDocuments("orders");

  var driverMap = {};
  drivers.forEach(function(d) { driverMap[d.id] = d.fields || {}; });

  var vehicleMap = {};
  vehicles.forEach(function(v) { vehicleMap[v.id] = v.fields || {}; });

  var orderMap = {};
  orders.forEach(function(o) { orderMap[o.id] = o.fields || {}; });

  var trackingRows = [];

  for (var i = 0; i < dispatches.length; i++) {
    var d = dispatches[i];
    var df = d.fields || {};

    var dispatchId = d.id;
    var orderId = getString(df["orderId"]);
    var orderNumber = getString(df["orderNumber"]);
    var customerName = getString(df["customerName"]);
    var driverId = getString(df["driverId"]);
    var driverName = getString(df["driverName"]);
    var vehicleId = getString(df["vehicleId"]);
    var vehicleReg = getString(df["vehicleRegistration"]);
    var pickupLoc = getString(df["pickupLocation"]);
    var delLoc = getString(df["deliveryLocation"]);
    var status = getString(df["status"]) || "PENDING";

    var drv = driverMap[driverId] || {};
    var veh = vehicleMap[vehicleId] || {};
    var ord = orderMap[orderId] || {};

    var driverPhone = getString(drv["phone"]) || getString(df["driverPhone"]);
    var vehicleType = getString(veh["vehicleType"]) || getString(df["vehicleType"]) || "VAN";
    var curLat = getNumber(drv["currentLatitude"]) || getNumber(df["currentLatitude"]) || 0;
    var curLng = getNumber(drv["currentLongitude"]) || getNumber(df["currentLongitude"]) || 0;
    var speed = getNumber(drv["speed"]) || 0;
    var heading = getNumber(drv["heading"]) || 0;
    var accuracy = getNumber(drv["accuracy"]) || 0;
    var lastActive = getTimestamp(drv["lastActive"]) || getTimestamp(df["updatedAt"]);
    
    // Check staleness (stale if > 5 min without update)
    var isStale = true;
    if (drv["lastActive"] && drv["lastActive"].timestampValue) {
      var diffMs = Date.now() - new Date(drv["lastActive"].timestampValue).getTime();
      isStale = diffMs > (5 * 60 * 1000);
    }

    var progress = 0.25;
    var eta = "Calculating...";
    if (status === "DELIVERED") {
      progress = 1.0;
      eta = "Delivered";
    } else if (status === "IN_TRANSIT" || status === "TRIP_STARTED") {
      progress = 0.65;
      eta = "In Transit (~30m)";
    } else if (status === "DISPATCH_CONFIRMED") {
      progress = 0.25;
      eta = "In Transit (~45m)";
    }

    var updatedAt = getTimestamp(df["updatedAt"]) || Utilities.formatDate(new Date(), Session.getScriptTimeZone() || "GMT+05:30", "yyyy-MM-dd HH:mm:ss");

    trackingRows.push([
      dispatchId,
      orderId,
      orderNumber,
      customerName,
      driverId,
      driverName,
      driverPhone,
      vehicleId,
      vehicleReg,
      vehicleType,
      pickupLoc,
      delLoc,
      status,
      curLat,
      curLng,
      speed,
      heading,
      accuracy,
      progress,
      eta,
      lastActive,
      isStale,
      updatedAt
    ]);
  }

  // Set Tracking Tab Headers & Values
  var trackingHeaders = [
    "Dispatch ID", "Order ID", "Order Number", "Customer Name", "Driver ID", "Driver Name",
    "Driver Phone", "Vehicle ID", "Vehicle Registration", "Vehicle Type", "Pickup Location",
    "Delivery Location", "Trip Status", "Current Latitude", "Current Longitude", "Speed (km/h)",
    "Heading", "Accuracy (m)", "Progress (%)", "ETA", "Last Location Update", "Is Stale", "Updated At"
  ];

  if (trackingSheet.getLastRow() > 1) {
    trackingSheet.getRange(2, 1, trackingSheet.getLastRow() - 1, trackingHeaders.length).clearContent();
  }

  trackingSheet.getRange(1, 1, 1, trackingHeaders.length).setValues([trackingHeaders]);
  trackingSheet.getRange(1, 1, 1, trackingHeaders.length).setFontWeight("bold").setBackground("#0F172A").setFontColor("#FFFFFF");
  trackingSheet.setFrozenRows(1);

  if (trackingRows.length > 0) {
    trackingSheet.getRange(2, 1, trackingRows.length, trackingHeaders.length).setValues(trackingRows);
  }

  return trackingRows.length;
}

/**
 * ----------------------------------------------------------------------------
 * 13. AUDIT LOGGING ENGINE (Backup Log TAB)
 * ----------------------------------------------------------------------------
 */
function logSyncOperation(collection, documentIdScope, operation, status, summaryDetails) {
  try {
    var ss = SpreadsheetApp.getActiveSpreadsheet();
    if (!ss) return;

    var logSheet = ss.getSheetByName(SHEET_TABS.BACKUP_LOG);
    if (!logSheet) {
      logSheet = ss.insertSheet(SHEET_TABS.BACKUP_LOG);
      logSheet.appendRow(["Timestamp", "Collection", "Document ID / Scope", "Operation", "Status", "Summary / Details"]);
      logSheet.getRange("A1:F1").setFontWeight("bold").setBackground("#0F172A").setFontColor("#FFFFFF");
      logSheet.setFrozenRows(1);
    }

    var timestamp = Utilities.formatDate(new Date(), Session.getScriptTimeZone() || "GMT+05:30", "yyyy-MM-dd HH:mm:ss");
    logSheet.appendRow([
      timestamp,
      collection || "SYSTEM",
      documentIdScope || "ALL",
      operation || "SYNC",
      status || "INFO",
      summaryDetails || ""
    ]);

    // Trim log to last 1000 entries
    var lastRow = logSheet.getLastRow();
    if (lastRow > 1005) {
      logSheet.deleteRows(2, lastRow - 1001);
    }
  } catch (e) {
    Logger.log("Failed to write to Backup Log: " + e.message);
  }
}

/**
 * ----------------------------------------------------------------------------
 * 14. EMPTY FIELD DIAGNOSTIC AUDIT TOOL
 * ----------------------------------------------------------------------------
 */
function runEmptyFieldAudit() {
  var startTime = new Date();
  Logger.log("🔍 Starting RouteCJ Empty Field Audit...");

  var ss = SpreadsheetApp.getActiveSpreadsheet();
  var auditTabName = "Audit Report";
  var auditSheet = ss.getSheetByName(auditTabName);
  if (!auditSheet) {
    auditSheet = ss.insertSheet(auditTabName);
  }
  auditSheet.clear();

  var auditHeaders = ["Collection", "Document ID", "Firebase Field", "Sheet Column", "Firebase Value", "Sheet Value", "Status"];
  auditSheet.appendRow(auditHeaders);
  auditSheet.getRange("A1:G1").setFontWeight("bold").setBackground("#0F172A").setFontColor("#38BDF8");
  auditSheet.setFrozenRows(1);

  var auditRows = [];
  var mismatchCount = 0;
  var totalFieldsChecked = 0;

  for (var tabName in SCHEMA_DEFINITIONS) {
    var schema = SCHEMA_DEFINITIONS[tabName];
    var docs = fetchFirestoreDocuments(schema.collection);
    var sheet = ss.getSheetByName(tabName);
    var indexMap = sheet ? buildDocumentIndexMap(sheet, schema) : {};

    for (var i = 0; i < docs.length; i++) {
      var doc = docs[i];
      var docId = doc.id;
      var f = doc.fields || {};

      var targetRow = indexMap[docId];
      var sheetRowValues = [];
      if (targetRow && sheet) {
        sheetRowValues = sheet.getRange(targetRow, 1, 1, schema.headers.length).getValues()[0];
      }

      for (var h = 0; h < schema.headers.length; h++) {
        var header = schema.headers[h];
        totalFieldsChecked++;

        var firestoreVal = extractFieldValue(docId, f, header, doc);
        var sheetVal = sheetRowValues.length > h ? sheetRowValues[h] : "";

        var strFirestore = String(firestoreVal !== null && firestoreVal !== undefined ? firestoreVal : "").trim();
        var strSheet = String(sheetVal !== null && sheetVal !== undefined ? sheetVal : "").trim();

        var status = "MATCH";
        if (strFirestore === "" && strSheet === "") {
          status = "EMPTY_IN_BOTH";
        } else if (strFirestore !== "" && strSheet === "") {
          status = "MISSING_IN_SHEET";
          mismatchCount++;
        } else if (strFirestore === "" && strSheet !== "") {
          status = "SHEET_ONLY";
        } else if (strFirestore !== strSheet) {
          // Check if timestamp formatting difference
          status = "VALUE_DIFF";
          mismatchCount++;
        }

        if (status !== "MATCH" && status !== "EMPTY_IN_BOTH") {
          auditRows.push([
            schema.collection,
            docId,
            header.field,
            header.name,
            strFirestore,
            strSheet,
            status
          ]);
        }
      }
    }
  }

  if (auditRows.length > 0) {
    auditSheet.getRange(2, 1, auditRows.length, auditHeaders.length).setValues(auditRows);
  }

  var elapsed = ((new Date().getTime() - startTime.getTime()) / 1000).toFixed(1);
  var report = "Audit Completed in " + elapsed + "s. Checked " + totalFieldsChecked + " fields across all collections. Found " + mismatchCount + " mismatches/missing values.";
  
  Logger.log("✅ " + report);
  SpreadsheetApp.getActiveSpreadsheet().toast(report, "🔍 Audit Complete", 8);
  logSyncOperation("ALL_COLLECTIONS", "AUDIT", "EMPTY_FIELD_AUDIT", "SUCCESS", report);

  return report;
}

/**
 * ----------------------------------------------------------------------------
 * 15. INITIAL BACKFILL & MIGRATION ENGINE
 * ----------------------------------------------------------------------------
 */
function runInitialDataMigration() {
  var startTime = new Date();
  Logger.log("🚀 Starting RouteCJ Initial Data Migration & Backfill...");

  setupSheets();
  var syncResult = syncAllCollections();

  var elapsed = ((new Date().getTime() - startTime.getTime()) / 1000).toFixed(1);
  var report = "Migration & Backfill Completed in " + elapsed + "s. " + syncResult.summary;

  logSyncOperation("ALL_COLLECTIONS", "INITIAL_IMPORT", "MIGRATION", "SUCCESS", report);
  
  SpreadsheetApp.getActiveSpreadsheet().toast(report, "🚀 Migration Complete", 8);
  Logger.log("✅ " + report);
  return report;
}

/**
 * ----------------------------------------------------------------------------
 * 16. SHEET INITIALIZATION & STYLING
 * ----------------------------------------------------------------------------
 */
function setupSheets() {
  var ss = SpreadsheetApp.getActiveSpreadsheet();
  if (!ss) return;

  for (var tabName in SCHEMA_DEFINITIONS) {
    var schema = SCHEMA_DEFINITIONS[tabName];
    var sheet = ss.getSheetByName(tabName);
    if (!sheet) {
      sheet = ss.insertSheet(tabName);
    }

    var headerRow = schema.headers.map(function(h) { return h.name; });
    sheet.getRange(1, 1, 1, headerRow.length).setValues([headerRow]);
    sheet.setFrozenRows(1);

    var headerRange = sheet.getRange(1, 1, 1, headerRow.length);
    headerRange.setFontWeight("bold").setBackground("#0F172A").setFontColor("#FFFFFF");

    for (var c = 0; c < schema.headers.length; c++) {
      var h = schema.headers[c];
      var cell = sheet.getRange(1, c + 1);
      if (h.protected) {
        cell.setFontColor("#94A3B8"); // Gray for protected
      } else {
        cell.setFontColor("#38BDF8"); // Cyan for editable
      }
    }
  }

  // Ensure Tracking tab exists
  updateTrackingTab();

  // Ensure Backup Log tab exists
  var logSheet = ss.getSheetByName(SHEET_TABS.BACKUP_LOG);
  if (!logSheet) {
    logSheet = ss.insertSheet(SHEET_TABS.BACKUP_LOG);
    logSheet.appendRow(["Timestamp", "Collection", "Document ID / Scope", "Operation", "Status", "Summary / Details"]);
    logSheet.getRange("A1:F1").setFontWeight("bold").setBackground("#0F172A").setFontColor("#FFFFFF");
    logSheet.setFrozenRows(1);
  }

  Logger.log("✅ All RouteCJ sheets initialized successfully!");
}

function getSheetOrInit(tabName, schema) {
  var ss = SpreadsheetApp.getActiveSpreadsheet();
  var sheet = ss.getSheetByName(tabName);
  if (!sheet) {
    setupSheets();
    sheet = ss.getSheetByName(tabName);
  }
  return sheet;
}

/**
 * ----------------------------------------------------------------------------
 * 17. UI MENU & TRIGGER AUTOMATION
 * ----------------------------------------------------------------------------
 */
function onOpen() {
  var ui = SpreadsheetApp.getUi();
  ui.createMenu("RouteCJ Sync")
    .addItem("🔄 Run Full Two-Way Sync", "syncAllCollections")
    .addItem("🚀 Run Initial Backfill / Migration", "runInitialDataMigration")
    .addItem("🔍 Run Empty Field Audit", "runEmptyFieldAudit")
    .addItem("🛠️ Setup All Sheets & Formatting", "setupSheets")
    .addSeparator()
    .addItem("🧪 Test Firestore Connection", "testFirestoreConnection")
    .addToUi();
}
