package com.routecj.admin.core.util

/**
 * Application-wide constants used throughout the application.
 * This follows the Single Responsibility Principle by centralizing all constants.
 */
object Constants {

    // App name and version
    const val APP_NAME = "RouteCJ Admin"
    const val APP_VERSION = "1.0"

    // Base URLs
    const val BASE_URL = "https://api.routecj.com/"

    // Database
    const val DATABASE_NAME = "routecj_admin.db"

    // SharedPreferences / DataStore
    const val PREFERENCES_NAME = "routecj_admin_prefs"

    // Timeout values (in seconds)
    const val NETWORK_TIMEOUT = 30L
    const val READ_TIMEOUT = 30L
    const val WRITE_TIMEOUT = 30L

    // Navigation routes
    object NavigationRoutes {
        const val SPLASH = "splash"
        const val LOGIN = "login"
        const val DASHBOARD = "dashboard"
        const val ORDERS = "orders"
        const val DRIVERS = "drivers"
        const val DRIVER_LOCATION = "driver_location/{driverId}"
        const val VEHICLES = "vehicles"
        const val VEHICLE_DETAILS = "vehicle_details/{vehicleId}"
        const val VEHICLE_LOGS = "vehicle_logs/{vehicleId}"
        const val ADD_VEHICLE = "add_vehicle"
        const val GODOWNS = "godowns"
        const val DISPATCH = "dispatch"
        const val TRACKING = "tracking"
        const val REPORTS = "reports"
        const val PROFILE = "profile"
        const val USER_MANAGEMENT = "user_management"
        const val NOTIFICATIONS = "notifications"
        const val SETTINGS = "settings"
        const val FORGOT_PASSWORD = "forgot_password"
        const val ADD_ORDER = "add_order"
        const val ORDER_DETAILS = "order_details/{orderId}"
        const val ADD_GODOWN = "add_godown"
        const val GODOWN_DETAILS = "godown_details/{godownId}"

        // Godown Manager Routes
        const val GODOWN_DASHBOARD = "godown_dashboard"
        const val INCOMING_PARCELS = "incoming_parcels"
        const val ADD_PARCEL = "add_parcel"
        const val PARCEL_DETAILS = "parcel_details/{parcelId}"
        const val QR_DISPLAY = "qr_display/{parcelId}"

        // Dispatch Manager Routes
        const val DISPATCH_DASHBOARD = "dispatch_dashboard"
        const val QR_SCANNER = "qr_scanner"
        const val VERIFIED_PARCEL_DETAILS = "verified_parcel_details/{parcelId}"
    }

    // Error messages
    object ErrorMessages {
        const val NETWORK_ERROR = "Network error. Please check your connection."
        const val SERVER_ERROR = "Server error. Please try again later."
        const val UNKNOWN_ERROR = "Unknown error occurred. Please try again."
        const val UNAUTHORIZED = "Unauthorized. Please login again."
    }

    // Response status codes
    object ResponseCodes {
        const val SUCCESS = 200
        const val CREATED = 201
        const val BAD_REQUEST = 400
        const val UNAUTHORIZED = 401
        const val FORBIDDEN = 403
        const val NOT_FOUND = 404
        const val SERVER_ERROR = 500
    }

    // Payment configuration
    object Payment {
        const val DEFAULT_UPI_ID = "manoj-2005-mekala@yes"
        const val DEFAULT_PAYEE_NAME = "RouteCJ Logistics (Manoj Mekala)"
    }
}

