# 🚚 ROUTECJ ADMIN DASHBOARD

## AI-Powered Supply Chain • Route Optimization • Real-Time Logistics Tracker

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

## 🌐 About RouteCJ Admin

**RouteCJ Admin** is the command center of the entire logistics management ecosystem designed to connect:

- 🏢 **Admin Operations & Godowns**
- 🚚 **Drivers & Fleet**
- 📱 **Customers**

into one synchronized logistics platform.

---

## 🏗️ Architecture & Tech Stack

- **MVVM Pattern**: Model-View-ViewModel architecture
- **Clean Architecture**: Separation of concerns with distinct domain, data, and presentation layers
- **Repository Pattern**: Single source of truth with Firebase Firestore real-time synchronization
- **Dependency Injection**: Hilt for DI
- **Jetpack Compose**: Modern declarative UI with Material Design 3
- **OpenStreetMap / osmdroid**: Open-source, zero-cost real-time map visualization

---

## 📱 Features

- 🔐 **Firebase Authentication & RBAC**: Role-based access control (Super Admin, Admin, Godown Manager, Dispatch Manager).
- 📦 **Order & Parcel Management**: Canonical address mapping across Customer, Admin, and Driver apps.
- 🏭 **Godown & QR Workflow**: Parcel intake, review, and instant QR generation.
- 🔄 **Dispatch Management**: QR scanning, driver & vehicle assignment, and automated dispatch.
- 🗺️ **Live Driver Tracking**: OpenStreetMap-based real-time tracking with speed, heading, and stale GPS detection.
- 🔐 **Secure Delivery OTP Verification**: Driver-side OTP verification with real-time Admin status updates.
- 📊 **Reports & Analytics**: Logistics performance metrics and analytics.

---

## 🚀 Getting Started

### Prerequisites
- Android Studio Ladybug or later
- JDK 17
- Android SDK API 26+ (Compiled against API 36)

### Build & Run
```bash
./gradlew assembleDebug
```

---

# 🚚 ROUTECJ — PLAN • DISPATCH • TRACK • DELIVER
