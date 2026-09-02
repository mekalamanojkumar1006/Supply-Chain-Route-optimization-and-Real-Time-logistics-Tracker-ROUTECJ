# Phase 0 Foundation Audit & Setup Plan

This plan outlines the steps to correct the package name, establish the project architecture, and configure the necessary dependencies for the RouteCJ Customer app.

## User Review Required

> [!IMPORTANT]
> The current `applicationId` and `namespace` are set to `com.routecj.comroutecjcustomer`. However, the existing `google-services.json` specifies `com.routecj.customer`. We will correct the project configuration to match `com.routecj.customer` to ensure Firebase compatibility.

## Proposed Changes

### Package & Configuration Correction

#### [MODIFY] [app/build.gradle.kts](file:///H:/adkprojectfile/comroutecjcustomer/app/build.gradle.kts)
- Update `namespace` to `com.routecj.customer`.
- Update `applicationId` to `com.routecj.customer`.
- Add Hilt and KSP plugins.
- Add Google Services plugin.
- Add required dependencies (Hilt, Navigation, Coroutines, Firebase).

#### [MODIFY] [gradle/libs.versions.toml](file:///H:/adkprojectfile/comroutecjcustomer/gradle/libs.versions.toml)
- Add versions and library definitions for Hilt, KSP, Navigation, and Firebase.

#### [MODIFY] [build.gradle.kts](file:///H:/adkprojectfile/comroutecjcustomer/build.gradle.kts) (root)
- Add Hilt and Google Services plugins to the `plugins` block.

#### [REFACTOR] Move Source Files
- Move `MainActivity.kt` from `com.routecj.comroutecjcustomer` to `com.routecj.customer`.
- Update package declarations in all moved files.

### Foundation Architecture

#### [NEW] Directory Structure
Create the following packages under `src/main/java/com/routecj/customer`:
- `core` (common, error, navigation, permissions, location, util)
- `data` (remote, local, repository, mapper)
- `domain` (model, repository, usecase)
- `presentation` (auth, home, orders, booking, tracking, notifications, profile, components)
- `di`

### Design Foundation

#### [MODIFY] Theme & Color Definitions
- Set up a premium, modern Material 3 design system foundation.
- Update `ui/theme` files (or create new ones in the corrected package).

### Firebase Integration

#### [MODIFY] Firebase Setup
- Ensure `google-services.json` is correctly recognized by applying the Google Services plugin.
- Add Firebase BOM and basic Firebase dependencies (Analytics/Auth foundation).

## Verification Plan

### Automated Tests
- Run `.\gradlew.bat app:assembleDebug` to verify the build and configuration.

### Manual Verification
- Verify that the generated R class and other build artifacts use the new package name `com.routecj.customer`.
