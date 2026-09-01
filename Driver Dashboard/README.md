# RouteCJ Driver App

A native Android driver application for the RouteCJ real-time logistics platform.

## Features

* Driver authentication (Login, Logout, Password Reset)
* Driver Home Dashboard & Metrics
* Driver Profile & Vehicle Details Management
* Trip Assignment & Trip Lifecycle (Start Trip, Complete Delivery)
* Customer Pickup Workflow & Arrival Confirmation
* Customer OTP Verification
* Parcel Details Entry & Godown Manager Submission
* Live GPS Tracking & Foreground Service with State Machine
* OpenStreetMap Integration (osmdroid)
* OSRM Driving Route Calculation & Offline Handling
* Turn-by-Turn Navigation Intent to Customer Pickup/Delivery Coordinates
* Firebase Cloud Messaging & Notification Center
* Trip History
* Clean Architecture & Jetpack Compose UI

## Technology

* Kotlin
* Jetpack Compose
* Clean Architecture
* Firebase Authentication
* Cloud Firestore
* Firebase Cloud Messaging
* Google Play Services Location
* OpenStreetMap / osmdroid
* OSRM

## Project Structure

```text
Driver Dashboard/
├── app/
├── gradle/
├── build.gradle.kts
├── settings.gradle.kts
└── README.md
```

## Build

```powershell
.\gradlew.bat :app:assembleDebug
```

## Testing

```powershell
.\gradlew.bat :app:testDebugUnitTest
```

## Security

Never commit:

* Firebase service-account credentials
* signing keys
* passwords
* private API credentials
* production secrets
