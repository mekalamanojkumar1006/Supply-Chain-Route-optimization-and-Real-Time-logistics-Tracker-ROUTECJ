
# 🚚 ROUTECJ

## AI-Powered Supply Chain • Route Optimization • Real-Time Logistics Tracking

<p align="center">
  <strong>One Platform. Three Apps. Complete Logistics Visibility.</strong>
</p>

<p align="center">
  📦 Parcel Management &nbsp; • &nbsp;
  🚚 Driver Operations &nbsp; • &nbsp;
  🗺️ Real-Time Tracking &nbsp; • &nbsp;
  📊 Analytics &nbsp; • &nbsp;
  🔐 Secure Role-Based Access
</p>

---

## 🌐 About RouteCJ

**RouteCJ** is an end-to-end logistics management ecosystem designed to connect:

- 🏢 **Admin Operations**
- 🚚 **Drivers**
- 📱 **Customers**

into one synchronized logistics platform.

The system manages the complete parcel journey:

```text
Customer
   ↓
Parcel Created
   ↓
Godown / Warehouse
   ↓
QR Verification
   ↓
Dispatch
   ↓
Driver Assignment
   ↓
Live GPS Tracking
   ↓
Delivery
   ↓
Driver OTP Verification
   ↓
Delivered
````

---

# 🏗️ ROUTECJ ECOSYSTEM

```text
                    ┌─────────────────────────┐
                    │        🚚 ROUTECJ       │
                    │   Logistics Ecosystem   │
                    └────────────┬────────────┘
                                 │
              ┌──────────────────┼──────────────────┐
              │                  │                  │
              ▼                  ▼                  ▼
      ┌───────────────┐  ┌───────────────┐  ┌───────────────┐
      │ 👨‍💼 ADMIN APP │  │ 🚚 DRIVER APP │  │ 📱 CUSTOMER APP│
      │               │  │               │  │               │
      │ Manage        │  │ Execute       │  │ Track         │
      │ Monitor       │  │ Deliver       │  │ Receive       │
      └───────┬───────┘  └───────┬───────┘  └───────┬───────┘
              │                  │                  │
              └──────────────────┼──────────────────┘
                                 ▼
                       ☁️ FIREBASE BACKEND
                                 │
                  ┌──────────────┼──────────────┐
                  ▼              ▼              ▼
             Firestore      Firebase Auth    Storage
```

---

# 📱 1. ADMIN DASHBOARD

The **RouteCJ Admin Dashboard** is the command center of the entire logistics operation.

### Admin Features

* 🔐 Secure Firebase Authentication
* 👥 Role-Based Access Control
* 📊 Operations Dashboard
* 📦 Order Management
* 🏭 Godown Management
* 🚚 Driver Management
* 🚛 Vehicle Management
* 🔄 Dispatch Management
* 📷 QR Code Scanning
* 🔳 QR Code Generation
* 🗺️ Live Driver Tracking
* 📍 Driver Location Monitoring
* 📈 Reports & Analytics
* 🔔 Notifications
* 👤 Admin Profile Management

### Admin Roles

| ID         | Role             | Responsibility                |
| ---------- | ---------------- | ----------------------------- |
| `ADMIN001` | Super Admin      | Complete system control       |
| `ADMIN002` | Admin            | Operational administration    |
| `ADMIN003` | Godown Manager   | Parcel intake & QR generation |
| `ADMIN004` | Dispatch Manager | QR scanning & dispatch        |

---

# 📦 GODOWN WORKFLOW

```text
ADMIN003
   ↓
Create / Receive Parcel
   ↓
Enter Parcel Details
   ↓
Review Parcel
   ↓
Generate QR
   ↓
READY FOR DISPATCH
```

---

# 🔳 QR DISPATCH WORKFLOW

```text
ADMIN003 — GODOWN
        ↓
   Generate QR
        ↓
   Physical Parcel
        ↓
ADMIN004 — DISPATCH
        ↓
   Scan QR
        ↓
 Parcel Verification
        ↓
 Assign Driver
        ↓
 Assign Vehicle
        ↓
     DISPATCH
```

---

# 🚚 2. DRIVER APP

The **RouteCJ Driver App** is designed for field delivery operations.

### Driver Features

* 🔐 Secure Driver Login
* 👤 Driver Profile
* 📦 Assigned Parcels
* 🚚 Assigned Trips
* ▶️ Start Trip
* 📍 Real-Time GPS
* 🗺️ Navigation
* 📦 Parcel Details
* 📞 Delivery Information
* 🔢 Delivery OTP
* ✅ Delivery Completion
* 🔄 Real-Time Status Synchronization

---

# 🔐 DRIVER DELIVERY VERIFICATION

**Only the Driver App can complete a delivery.**

The Admin application does **not** contain a "Complete Delivery" action.

```text
Driver receives parcel
        ↓
Starts trip
        ↓
GPS tracking begins
        ↓
Travels to destination
        ↓
Customer receives parcel
        ↓
Driver enters Delivery OTP
        ↓
OTP verified
        ↓
DELIVERED
```

After the Driver submits the correct OTP, the delivery status synchronizes through Firebase to authorized RouteCJ applications.

---

# 📱 3. CUSTOMER APP

The **RouteCJ Customer App** provides customers with visibility into their parcels.

### Customer Features

* 🔐 Customer Account
* 📦 Parcel Information
* 🔎 Parcel Tracking
* 🚚 Trip Status
* 📍 Delivery Progress
* 🗺️ Live Location
* ⏱️ Estimated Delivery
* 🔔 Delivery Updates
* ✅ Delivery Confirmation

---

# 🗺️ REAL-TIME LOCATION TRACKING

```text
Driver GPS
    ↓
drivers/{driverId}
    ↓
Firebase Firestore
    ↓
Tracking Repository
    ↓
Tracking ViewModel
    ↓
Live Map
```

The tracking system can display:

* 🚚 Driver location
* 💨 Speed
* 🧭 Heading
* 📍 GPS accuracy
* 🕐 Last active time
* 📦 Current parcel
* 🚛 Assigned vehicle
* 📊 Trip progress
* 🟢 Active status
* 🔴 Offline / stale GPS status

---

# 🗺️ MAP & NAVIGATION

RouteCJ includes map-based logistics visualization.

```text
             📍 PICKUP / STORE
                    │
                    ▼
                  🚚
               DRIVER
                    │
                    ▼
             📍 DESTINATION
```

The Admin tracking system uses an OpenStreetMap-based implementation for cost-effective map visualization.

---

# 📊 REPORTS & ANALYTICS

The Admin Dashboard provides:

* 📦 Total Orders
* ✅ Delivered Orders
* 🚚 Active Trips
* ⏳ Pending Orders
* ❌ Cancelled / Failed Orders
* 📈 Delivery Performance
* 👨‍✈️ Driver Performance
* 🚛 Vehicle Utilization
* 🏭 Godown Capacity
* 📊 Order Status Distribution
* 📅 Daily Order Trends
* 📁 CSV Export

---

# 🔄 REAL-TIME DATA FLOW

```text
                         ☁️ FIREBASE
                              │
          ┌───────────────────┼───────────────────┐
          ▼                   ▼                   ▼
       Orders             Dispatches           Drivers
          │                   │                   │
          └───────────────────┼───────────────────┘
                              ▼
                       Real-Time Sync
                              │
          ┌───────────────────┼───────────────────┐
          ▼                   ▼                   ▼
        👨‍💼 Admin            🚚 Driver          📱 Customer
          App                 App                App
```

---

# 🔐 SECURITY & RBAC

RouteCJ uses Firebase Authentication and Firestore Security Rules.

* 🔐 Authenticated access
* 🛡️ Role-Based Access Control
* 🔑 Firebase Authentication
* 🔒 Firestore Security Rules
* 👤 Identity-based profile mapping
* 🚫 No public database access
* 🚫 No client-side role escalation
* 🚫 No Admin delivery completion
* 🔏 Driver-only OTP delivery completion

---

# 🧩 PROJECT STRUCTURE

```text
ROUTECJ/
│
├── 📁 Admin Dashboard/
│   ├── app/
│   ├── gradle/
│   ├── firestore.rules
│   ├── storage.rules
│   └── README.md
│
├── 📁 Driver Dashboard/
│   └── Driver Application
│
├── 📁 Customer Dashboard/
│   └── Customer Application
│
└── 📄 README.md
```

---

# 🧱 ARCHITECTURE

```text
Presentation Layer
        ↓
ViewModel
        ↓
Use Cases
        ↓
Repository
        ↓
Firebase / Data Layer
        ↓
Firestore / Storage / Authentication
```

---

# 🧩 TECHNOLOGY STACK

### Android

* Kotlin
* Jetpack Compose
* Material Design
* CameraX
* Android Architecture Components

### Backend

* Firebase Authentication
* Cloud Firestore
* Firebase Storage
* Firebase Security Rules

### Maps

* OpenStreetMap
* osmdroid
* GPS
* Real-Time Location Updates

### Architecture

* Clean Architecture
* MVVM
* Repository Pattern
* Use Cases
* Dependency Injection
* Reactive Data Streams

---

# 🎯 COMPLETE ROUTECJ FLOW

```text
ADMIN003
Create / Choose Parcel
        ↓
Generate QR
        ↓
ADMIN004
Scan Physical QR
        ↓
Parcel Verified
        ↓
Assign Driver
        ↓
Assign Vehicle
        ↓
Dispatch
        ↓
DRIVER APP
Start Trip
        ↓
Live GPS
        ↓
Customer Delivery
        ↓
Driver Enters Delivery OTP
        ↓
OTP Verified
        ↓
DELIVERED
        ↓
Automatic Real-Time Update
        ↓
ADMIN + CUSTOMER
```

---

# 🚀 PROJECT GOALS

### ⚡ Faster Operations

Automate parcel, dispatch and delivery workflows.

### 📍 Better Visibility

Know where active deliveries are in real time.

### 🔐 Better Security

Use authentication, RBAC, QR verification and Driver OTP.

### 📊 Better Decisions

Provide operational analytics and reports.

### 🤝 Better Coordination

Connect Admins, Drivers, Godowns and Customers through one ecosystem.

### 💰 Cost-Effective Tracking

Use OpenStreetMap-based mapping to reduce dependence on paid map services.

---

# 🌟 WHY ROUTECJ?

> **RouteCJ connects the entire delivery ecosystem — from warehouse to doorstep.**

📦 **Manage parcels**

🚚 **Manage drivers**

🚛 **Manage vehicles**

🏭 **Manage warehouses**

🔳 **Verify with QR**

🗺️ **Track deliveries**

📍 **Monitor GPS**

🔢 **Secure delivery with OTP**

📊 **Analyze operations**

🔄 **Synchronize everything in real time**

---

# 📈 DEVELOPMENT STATUS

### Core Logistics Workflow

* [x] Firebase Authentication
* [x] Admin Role-Based Access
* [x] Parcel Management
* [x] Manual Parcel Creation
* [x] Customer Parcel Intake
* [x] QR Generation
* [x] QR Scanning
* [x] Parcel Verification
* [x] Driver Assignment
* [x] Vehicle Assignment
* [x] Dispatch Creation
* [x] Driver-Only Delivery Completion
* [x] Delivery OTP
* [x] Real-Time Admin Synchronization
* [x] Live Driver Tracking
* [x] Reports & Analytics
* [x] OpenStreetMap Integration

### Applications

* [x] Admin Dashboard
* [ ] Driver Dashboard — ongoing development
* [ ] Customer Dashboard — ongoing development

---

# 🚀 FUTURE VISION

RouteCJ is designed to evolve into a complete intelligent logistics platform with:

* 🤖 AI-assisted route optimization
* 📍 Advanced live tracking
* 🧠 Predictive delivery ETA
* 📊 Advanced logistics intelligence
* 🚦 Traffic-aware routing
* 📦 Smart parcel management
* 🔔 Intelligent notifications
* 📈 Predictive fleet analytics
* 🌐 Scalable multi-location operations

---

# 🚚 ROUTECJ

### PLAN • DISPATCH • TRACK • DELIVER

**📦 📍 🚚 🗺️ 🔐 📊**

### One Platform. Three Apps. Complete Logistics Visibility.

```

**Key point:** the `[svg](https://github.com/.../new/main?...` text you showed is **not part of this README**. If you see it while editing on GitHub, you're likely looking at GitHub's generated heading/anchor UI rather than raw Markdown.
```
