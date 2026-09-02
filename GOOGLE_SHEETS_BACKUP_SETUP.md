# RouteCJ — Two-Way Firebase Firestore ↔ Google Sheets Synchronization Setup & Reference Guide

This guide provides step-by-step instructions to set up the **100% Free Automatic Two-Way Synchronization Engine** between **Firebase Firestore** and your master **Google Spreadsheet** (`ROUTECJ DATABASE BACKUP` / ID: `17nwnNtKfBcw8zr2elXcAn3mbLS-PtLrSj9vIi4RJMLw`).

---

## 1. System Architecture

```
                  ┌─────────────────────────────────────────────────────────┐
                  │                 Firebase Firestore                      │
                  │                 (Primary Database)                      │
                  │  admins | drivers | vehicles | godowns | orders | ...   │
                  └──────────────▲───────────────────────────┬──────────────┘
                                 │                           │
                   REST Patch    │                           │ Time-Triggered Sync
                 (onEdit Trigger)│                           │ & Firestore REST Pull
                                 │                           │
                  ┌──────────────┴───────────────────────────▼──────────────┐
                  │          Google Apps Script Sync Engine                 │
                  │    (OAuth 2.0 Service Account JWT + Validation)         │
                  │  Loop Prevention • Deduplication • Multi-Alias Parser   │
                  └──────────────▲───────────────────────────┬──────────────┘
                                 │                           │
                                 │ User Edits                │ Sheet R/W & In-Place Upsert
                                 │ (Permitted Fields)        │ (Protected Columns Locked)
                                 │                           │
                  ┌──────────────┴───────────────────────────▼──────────────┐
                  │         Google Spreadsheet (Master Operations Mirror)   │
                  │ Orders | Dispatches | Drivers | Vehicles | Godowns |    │
                  │ Admins | Tracking | Backup Log                          │
                  └─────────────────────────────────────────────────────────┘
```

---

## 2. Fast 3-Minute Setup

### Step 1: Open Master Google Sheet
1. Open Google Sheets:
   `https://docs.google.com/spreadsheets/d/17nwnNtKfBcw8zr2elXcAn3mbLS-PtLrSj9vIi4RJMLw/edit`

### Step 2: Open Apps Script Editor
1. In the Google Sheet top menu, click **Extensions** $\rightarrow$ **Apps Script**.
2. Replace all existing code in `Code.gs` with the entire contents of [`RouteCJ_Sync_Script.gs`](./RouteCJ_Sync_Script.gs).
3. Click the **Save** (💾) icon.

### Step 3: Configure Secure Script Properties
1. In Apps Script left sidebar, click **Project Settings** (⚙️).
2. Scroll down to **Script Properties** and click **Add script property**.
3. Add the following keys:

| Property Name | Example Value | Description |
| :--- | :--- | :--- |
| `FIREBASE_PROJECT_ID` | `supplychaintracking-21492` | Your Firebase Project ID |
| `FIREBASE_CLIENT_EMAIL` | `firebase-adminsdk-xxxxx@...` | Service Account Client Email |
| `FIREBASE_PRIVATE_KEY` | `-----BEGIN PRIVATE KEY-----\nMIIEvgIB...` | Service Account Private Key |
| `AUTH_SECRET_TOKEN` | `RouteCJ_Sync_Token_2026_Secured` | Webhook / API secret token |

4. Click **Save script properties**.

---

## 3. Configuring Triggers (Automated Sync)

### A. Setup `onEdit` Trigger (Sheets $\rightarrow$ Firebase)
1. In the Apps Script left sidebar, click **Triggers** (⏰ icon).
2. Click **+ Add Trigger** (bottom right).
3. Configure the trigger:
   * **Choose which function to run**: `onEditTrigger`
   * **Choose which deployment should run**: `Head`
   * **Select event source**: `From spreadsheet`
   * **Select event type**: `On edit`
4. Click **Save**.

### B. Setup Time-Driven Trigger (Firebase $\rightarrow$ Sheets)
1. Click **+ Add Trigger**.
2. Configure the trigger:
   * **Choose which function to run**: `syncAllCollections`
   * **Choose which deployment should run**: `Head`
   * **Select event source**: `Time-driven`
   * **Select type of time based trigger**: `Minutes timer`
   * **Select minute interval**: `Every 1 minute` (or `Every 5 minutes`)
3. Click **Save**.

---

## 4. Master Spreadsheet Tab & Column Structure

The sync engine is aligned 100% with the 8 tabs of the master spreadsheet:

### 1. `Orders` (30 Columns)
`Order ID` | `Order Number` | `Customer Name` | `Customer Phone` | `Item Name` | `Quantity` | `Weight (kg/tons)` | `Pickup Location` | `Pickup Pincode` | `Delivery Location` | `Delivery Pincode` | `Status` | `Priority` | `Payment Status` | `Total Amount` | `Driver ID` | `Driver Name` | `Vehicle ID` | `Vehicle Registration` | `Parcel ID` | `QR ID` | `OTP Verified` | `Delivered At` | `Delivered By` | `Delivery Remarks` | `Created By` | `Created By Role` | `Source` | `Created At` | `Updated At`

### 2. `Dispatches` (16 Columns)
`Dispatch ID` | `Order ID` | `Order Number` | `Customer Name` | `Driver ID` | `Driver Name` | `Vehicle ID` | `Vehicle Registration` | `Pickup Location` | `Delivery Location` | `Status` | `Priority` | `Estimated Delivery` | `Remarks` | `Created At` | `Updated At`

### 3. `Drivers` (18 Columns)
`Driver ID` | `Firebase UID` | `Driver Name` | `Email` | `Phone` | `Status` | `License Number` | `License Expiry` | `Assigned Vehicle` | `Rating` | `Total Deliveries` | `Completed Deliveries` | `Current Latitude` | `Current Longitude` | `Speed (km/h)` | `Heading` | `Last Active` | `Created At`

### 4. `Vehicles` (18 Columns)
`Vehicle ID` | `Registration Number` | `Vehicle Type` | `Make / Brand` | `Model` | `Capacity` | `Capacity Unit` | `Status` | `Assigned Driver ID` | `Assigned Driver Name` | `Fuel Level (%)` | `Odometer (km)` | `Image URL` | `Last Service Date` | `Next Service Date` | `Insurance Expiry` | `Created At` | `Updated At`

### 5. `Godowns` (16 Columns)
`Godown ID` | `Godown Name` | `Address` | `City` | `State` | `Pincode` | `Latitude` | `Longitude` | `Capacity (Tons)` | `Current Stock (Tons)` | `Manager ID` | `Manager Name` | `Contact Phone` | `Status` | `Created At` | `Updated At`

### 6. `Admins` (9/10 Columns)
`Admin ID` | `Firebase UID` | `Admin Name` | `Email` | `Phone` | `Role` | `Status` | `Last Login` | `Profile Image URL` | `Updated At`

### 7. `Tracking` (23 Columns)
`Dispatch ID` | `Order ID` | `Order Number` | `Customer Name` | `Driver ID` | `Driver Name` | `Driver Phone` | `Vehicle ID` | `Vehicle Registration` | `Vehicle Type` | `Pickup Location` | `Delivery Location` | `Trip Status` | `Current Latitude` | `Current Longitude` | `Speed (km/h)` | `Heading` | `Accuracy (m)` | `Progress (%)` | `ETA` | `Last Location Update` | `Is Stale` | `Updated At`

### 8. `Backup Log` (6 Columns)
`Timestamp` | `Collection` | `Document ID / Scope` | `Operation` | `Status` | `Summary / Details`

---

## 5. Last Login Workflow

1. Admin authenticates in RouteCJ Android App (`FirebaseAuth.signInWithEmailAndPassword`).
2. Upon success, Android app writes `lastLogin = FieldValue.serverTimestamp()` and `updatedAt = FieldValue.serverTimestamp()` to Firestore document (`admins/{uid}` and `admins/{adminId}`).
3. Automatic Apps Script sync pulls the timestamp, formats it as `yyyy-MM-dd HH:mm:ss`, and updates the `Last Login` column in the `Admins` sheet.
4. If sync is temporarily unavailable, login completes normally without failure.

---

## 6. Diagnostic Tools

### Empty Field Audit
1. In Google Sheets, click the **RouteCJ Sync** menu.
2. Select **🔍 Run Empty Field Audit**.
3. The script inspects all Firestore collections, compares every field against the sheet, and generates a diagnostic sheet: `Audit Report` detailing:
   `Collection` | `Document ID` | `Firebase Field` | `Sheet Column` | `Firebase Value` | `Sheet Value` | `Status`.
