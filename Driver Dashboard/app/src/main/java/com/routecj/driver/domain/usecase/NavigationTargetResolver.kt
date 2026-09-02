package com.routecj.driver.domain.usecase

import com.routecj.driver.domain.model.LocationAccuracy
import com.routecj.driver.domain.model.StoreLocation
import com.routecj.driver.domain.model.TripDetails
import org.osmdroid.util.GeoPoint

/**
 * Target type enumeration for driver map navigation.
 */
enum class NavigationTargetType {
    CUSTOMER_PICKUP,
    CUSTOMER_DELIVERY,
    STORE,
    NONE
}

/**
 * Encapsulates the resolved target point and display metadata for driver navigation.
 */
data class NavigationTarget(
    val type: NavigationTargetType,
    val point: GeoPoint?,
    val label: String,
    val address: String,
    val pincode: String = "",
    val city: String = "",
    val area: String = "",
    val street: String = "",
    val landmark: String = "",
    val hasValidCoordinates: Boolean = false,
    val accuracy: LocationAccuracy = LocationAccuracy.UNAVAILABLE,
    val accuracyBadgeText: String = "LOCATION NOT AVAILABLE"
)

/**
 * Centralized target-selection logic for RouteCJ Driver navigation.
 *
 * Enforces Phase 30 Rules:
 * 1. Before pickup/OTP verification: TARGET = CUSTOMER PICKUP (orders.origin.latitude/longitude)
 * 2. After pickup/OTP verification: TARGET = CUSTOMER DELIVERY (orders.destination.latitude/longitude)
 * 3. Store location (18.1085, 83.3988) remains completely separate and is ONLY used when store is explicitly requested or no trip is active.
 * 4. PINCODE & CITY CENTERS ARE NEVER TREATED AS EXACT LOCATION:
 *    Displays clear accuracy badges ("LOCATION VERIFIED", "APPROXIMATE LOCATION", "LOCATION NOT AVAILABLE").
 * 5. NO FAKE CUSTOMER LOCATION: Missing or invalid customer coordinates (0.0, 0.0) never fall back to store or city center.
 */
object NavigationTargetResolver {

    /**
     * Checks whether the given coordinates are valid non-zero latitude and longitude.
     */
    fun isValidCoordinate(lat: Double, lng: Double): Boolean {
        return lat != 0.0 && lng != 0.0 && lat >= -90.0 && lat <= 90.0 && lng >= -180.0 && lng <= 180.0
    }

    /**
     * Returns a human-readable badge label for the given accuracy level.
     */
    fun getAccuracyBadgeText(accuracy: LocationAccuracy): String {
        return when (accuracy) {
            LocationAccuracy.EXACT -> "LOCATION VERIFIED"
            LocationAccuracy.APPROXIMATE -> "APPROXIMATE LOCATION"
            LocationAccuracy.UNAVAILABLE -> "LOCATION NOT AVAILABLE"
        }
    }

    /**
     * Determines whether the pickup phase of a trip has been completed.
     */
    fun isPickupCompleted(trip: TripDetails): Boolean {
        if (trip.otpVerified) return true
        val upperStatus = trip.status.uppercase()
        return upperStatus in listOf(
            "PICKED_UP",
            "PENDING_GODOWN_REVIEW",
            "QR_GENERATED",
            "READY_FOR_DISPATCH",
            "DISPATCHED",
            "IN_TRANSIT",
            "DELIVERED",
            "COMPLETED"
        )
    }

    /**
     * Centralized target-selection function.
     *
     * @param trip Active trip details or null.
     * @param selectedStore Currently selected store location (or default).
     * @param requiresStore Explicit flag indicating the workflow requires store travel.
     */
    fun resolveTarget(
        trip: TripDetails?,
        selectedStore: StoreLocation? = null,
        requiresStore: Boolean = false
    ): NavigationTarget {
        if (trip == null) {
            val store = selectedStore ?: StoreLocation.VIZIANAGARAM_STORE
            val valid = isValidCoordinate(store.latitude, store.longitude)
            return NavigationTarget(
                type = NavigationTargetType.STORE,
                point = if (valid) GeoPoint(store.latitude, store.longitude) else null,
                label = store.name,
                address = store.address,
                hasValidCoordinates = valid,
                accuracy = LocationAccuracy.EXACT,
                accuracyBadgeText = "STORE LOCATION"
            )
        }

        if (requiresStore) {
            val store = selectedStore ?: StoreLocation.VIZIANAGARAM_STORE
            val valid = isValidCoordinate(store.latitude, store.longitude)
            return NavigationTarget(
                type = NavigationTargetType.STORE,
                point = if (valid) GeoPoint(store.latitude, store.longitude) else null,
                label = "RouteCJ Store",
                address = store.address,
                hasValidCoordinates = valid,
                accuracy = LocationAccuracy.EXACT,
                accuracyBadgeText = "STORE LOCATION"
            )
        }

        val pickupDone = isPickupCompleted(trip)

        return if (!pickupDone) {
            val valid = isValidCoordinate(trip.originLat, trip.originLng)
            val accuracy = if (valid) LocationAccuracy.EXACT else LocationAccuracy.UNAVAILABLE
            NavigationTarget(
                type = NavigationTargetType.CUSTOMER_PICKUP,
                point = if (valid) GeoPoint(trip.originLat, trip.originLng) else null,
                label = "CUSTOMER PICKUP",
                address = trip.pickupAddress,
                hasValidCoordinates = valid,
                accuracy = accuracy,
                accuracyBadgeText = getAccuracyBadgeText(accuracy)
            )
        } else {
            val valid = isValidCoordinate(trip.destinationLat, trip.destinationLng)
            val accuracy = if (valid) LocationAccuracy.EXACT else LocationAccuracy.UNAVAILABLE
            NavigationTarget(
                type = NavigationTargetType.CUSTOMER_DELIVERY,
                point = if (valid) GeoPoint(trip.destinationLat, trip.destinationLng) else null,
                label = "CUSTOMER DELIVERY",
                address = trip.deliveryAddress,
                hasValidCoordinates = valid,
                accuracy = accuracy,
                accuracyBadgeText = getAccuracyBadgeText(accuracy)
            )
        }
    }
}
