# Implementation Plan - Role-Specific Workflows (Godown & Dispatch)

This plan outlines the steps to implement dedicated workflows for Godown Managers and Dispatch Managers, including QR generation and scanning, while maintaining the existing Super Admin and Admin functionality.

## Proposed Changes

### 1. Domain Model Updates

#### [MODIFY] [Order.kt](file:///H:/adkprojectfile/RouteCJAdmin/app/src/main/java/com/routecj/admin/domain/model/Order.kt)
- Update `OrderStatus` enum with: `PENDING_GODOWN_REVIEW`, `QR_GENERATED`, `READY_FOR_DISPATCH`, `DISPATCHED`.
- Add new fields to `Order` data class:
    - `otpVerified: Boolean = false`
    - `qrId: String? = null`
    - `qrStatus: String? = null`
    - `qrGeneratedBy: String? = null`
    - `qrGeneratedAt: Date? = null`
    - `verificationToken: String? = null`
    - `parcelId: String? = null` (Optional identifier for parcel)

#### [MODIFY] [DashboardMetrics.kt](file:///H:/adkprojectfile/RouteCJAdmin/app/src/main/java/com/routecj/admin/domain/model/DashboardMetrics.kt)
- Add fields for role-specific dashboard counters:
    - `pendingGodownReview`, `qrGeneratedCount`, `receivedCount`, `readyForDispatchCount` (Godown)
    - `pendingDispatchCount`, `activeDispatchTrips`, `availableDriversForDispatch`, `availableVehiclesForDispatch` (Dispatch)

### 2. Core Security & Navigation

#### [MODIFY] [Constants.kt](file:///H:/adkprojectfile/RouteCJAdmin/app/src/main/java/com/routecj/admin/core/util/Constants.kt)
- Add new navigation routes for role-specific screens:
    - `GODOWN_DASHBOARD`, `INCOMING_PARCELS`, `PARCEL_DETAILS`, `QR_DISPLAY`
    - `DISPATCH_DASHBOARD`, `QR_SCANNER`, `VERIFIED_PARCEL_DETAILS`

#### [MODIFY] [NavGraph.kt](file:///H:/adkprojectfile/RouteCJAdmin/app/src/main/java/com/routecj/admin/presentation/navigation/NavGraph.kt)
- Register the new routes and their corresponding screens.
- Implement role-based redirection logic if necessary (e.g., in `LoginScreen` or `SplashScreen`).

### 3. Data Layer Updates

#### [MODIFY] [FirestoreOrderRepository.kt](file:///H:/adkprojectfile/RouteCJAdmin/app/src/main/java/com/routecj/admin/data/repository/FirestoreOrderRepository.kt)
- Update `docToOrder` and `orderToMap` to handle the new parcel and QR fields.
- Add methods to update parcel status and QR info.

#### [MODIFY] [DashboardRepositoryImpl.kt](file:///H:/adkprojectfile/RouteCJAdmin/app/src/main/java/com/routecj/admin/data/repository/DashboardRepositoryImpl.kt)
- Update the order snapshot listener to calculate the new role-specific metrics based on `OrderStatus`.

### 4. Presentation Layer (UI & ViewModels)

#### [NEW] [Godown Manager Workflows]
- `GodownDashboardScreen` & `GodownDashboardViewModel`
- `IncomingParcelsScreen` & `IncomingParcelsViewModel` (List of parcels in `PENDING_GODOWN_REVIEW`)
- `ParcelDetailsScreen` (Details view with "Generate QR" button)
- `QRDisplayScreen` (Shows generated QR code)

#### [NEW] [Dispatch Manager Workflows]
- `DispatchDashboardScreen` & `DispatchDashboardViewModel`
- `QRScannerScreen` (Camera-based QR scanner using CameraX or similar)
- `VerifiedParcelDetailsScreen` (Details view after successful scan with Driver/Vehicle selection)

#### [MODIFY] [DashboardScreen.kt](file:///H:/adkprojectfile/RouteCJAdmin/app/src/main/java/com/routecj/admin/presentation/dashboard/DashboardScreen.kt)
- Make the dashboard adaptive to show the correct UI based on the user's role.

### 5. Security Rules

#### [MODIFY] [firestore.rules](file:///H:/adkprojectfile/RouteCJAdmin/firestore.rules)
- Add rules to allow Godown Managers to update order status to `READY_FOR_DISPATCH`.
- Add rules to allow Dispatch Managers to create dispatches and update order status to `DISPATCHED`.

## Verification Plan

### Automated Tests
- N/A (Manual verification on device is required for QR scanning and real-time Firestore updates).

### Manual Verification
1. **Godown Manager Test**:
    - Login as Godown Manager.
    - Verify Dashboard shows metrics like "Pending Review".
    - Navigate to "Incoming Parcels".
    - Select a parcel, review details, and tap "Generate QR".
    - Verify QR is generated and status in Firestore changes to `READY_FOR_DISPATCH`.
2. **Dispatch Manager Test**:
    - Login as Dispatch Manager.
    - Verify Dashboard shows "Ready for Dispatch".
    - Use QR Scanner to scan a generated QR.
    - Verify parcel details are displayed correctly.
    - Select Driver and Vehicle, then tap "Create Dispatch".
    - Verify Dispatch trip is created in Firestore and Order status changes to `DISPATCHED`.
3. **Super Admin/Admin Test**:
    - Login as Super Admin.
    - Verify that the standard dashboard and all management features (Orders, Drivers, etc.) still work as expected.
