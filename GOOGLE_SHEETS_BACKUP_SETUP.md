# RouteCJ — Two-Way Firebase Firestore ↔ Google Sheets Synchronization Setup Guide

This guide provides step-by-step instructions to set up the **100% Free Automatic Two-Way Synchronization Engine** between **Firebase Firestore** and your master **Google Spreadsheet** (`ROUTECJ DATABASE BACKUP`).

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
                  │   Loop Prevention • Conflict Handler • SyncLog Engine   │
                  └──────────────▲───────────────────────────┬──────────────┘
                                 │                           │
                                 │ User Edits                │ Sheet R/W & Styling
                                 │ (Permitted Fields)        │ (Protected Columns Locked)
                                 │                           │
                  ┌──────────────┴───────────────────────────▼──────────────┐
                  │         Google Spreadsheet (Master Admin Mirror)        │
                  │ Admins | Drivers | Vehicles | Godowns | Orders | SyncLog│
                  └─────────────────────────────────────────────────────────┘
```

---

## 2. Fast 3-Minute Setup

### Step 1: Create or Open Master Google Sheet
1. Open Google Sheets at [sheets.google.com](https://sheets.google.com).
2. Create a new blank spreadsheet.
3. Rename the sheet to: **`ROUTECJ DATABASE BACKUP`**.

### Step 2: Open Apps Script Editor
1. In the Google Sheet top menu, click **Extensions** $\rightarrow$ **Apps Script**.
2. Replace all existing code in `Code.gs` with the entire contents of [`RouteCJ_Backup_Script.gs`](./RouteCJ_Backup_Script.gs).
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

## 4. Run Initial Data Migration
1. Go back to your Google Sheet and reload the page.
2. You will see a new menu: **RouteCJ Sync**.
3. Click **RouteCJ Sync** $\rightarrow$ **🚀 Run Initial Data Migration**.
4. The system will automatically:
   * Initialize all 8 tabs (`Admins`, `Drivers`, `Vehicles`, `Godowns`, `Orders`, `Dispatches`, `Reports`, `SyncLog`).
   * Apply visual header formatting (Protected columns marked with `🔒` in muted gray; editable columns marked in cyan).
   * Pull all existing Firestore data without duplicates.
   * Write an initial migration summary to the **SyncLog** tab.

---

## 5. Protected vs. Editable Fields Matrix

| Tab | Editable Administrative Fields (Syncs to Firebase) | Strictly Protected / Read-Only Fields (Live Ops) |
| :--- | :--- | :--- |
| **`Vehicles`** | `Registration Number`, `Vehicle Type`, `Brand`, `Model`, `Capacity`, `Fuel Level`, `Status`, `Service Date`, `Insurance Expiry` | `Current Latitude`, `Current Longitude`, `Speed`, `Odometer`, `Updated At` |
| **`Godowns`** | `Godown Name`, `Address`, `City`, `State`, `Pincode`, `Latitude`, `Longitude`, `Capacity`, `Manager ID`, `Phone`, `Status` | `Current Stock`, `Created At`, `Updated At` |
| **`Drivers`** | `Driver Name`, `Phone`, `Status`, `License Number`, `License Expiry`, `Assigned Vehicle ID` | `Firebase UID`, `Email`, `Role`, `Current Latitude`, `Current Longitude`, `Speed`, `Heading`, `Last Active` |
| **`Orders`** | `Customer Name`, `Customer Phone`, `Item Name`, `Quantity`, `Weight`, `Pickup Address`, `Delivery Address`, `Priority`, `Payment Status`, `Payment Method`, `Payment Amount`, `Transaction ID`, `Payment Notes`, `Remarks` | `Status` (`DELIVERED`, `IN_TRANSIT`, `DISPATCHED`), `Parcel ID`, `QR ID`, `OTP Verified`, `Delivery OTP`, `Delivered At`, `Delivered By` |
| **`Admins`** | `Admin Name`, `Phone`, `Status`, `Profile Image URL` | `Admin ID`, `Firebase UID`, `Email`, `Role`, `Last Login` |
| **`Dispatches`**| `Driver ID`, `Vehicle ID`, `Priority`, `Estimated Delivery`, `Remarks` | `Dispatch ID`, `Order ID`, `Status`, `Created At`, `Updated At` |

---

## 6. Safety & Conflict Handling Rules

1. **Loop Prevention**: Programmatic mutation locks (`ROUTECJ_PROGRAMMATIC_MUTATION_LOCK`) and metadata tag `syncSource = "GOOGLE_SHEETS"` guarantee no infinite bounce.
2. **Validation Failure**: If an administrator inputs an invalid coordinate (e.g. `latitude = 120.0`) or invalid phone/pincode, the cell automatically reverts to its prior value and logs a warning in `SyncLog`.
3. **Protected Edit Rejection**: If an administrator attempts to overwrite a protected live field (e.g. live GPS or delivery OTP), the cell immediately restores the true Firestore value and alerts the user via a Toast.
4. **Offline Resiliency**: If Google Sheets is unavailable, the RouteCJ Android applications continue functioning with 100% full capabilities directly with Firebase Firestore.
