package com.routecj.admin.domain.model

import java.util.Date

/**
 * Consolidated model for real-time trip tracking.
 */
data class TrackingInfo(
    val dispatchId: String,
    val orderId: String,
    val orderNumber: String,
    val customerName: String,
    val status: DispatchStatus,
    val pickupLocation: String,
    val deliveryLocation: String,
    val driverId: String,
    val driverName: String,
    val driverPhone: String = "",
    val vehicleId: String,
    val vehicleRegistration: String,
    val vehicleType: String = "Truck",
    val itemName: String = "Freight Cargo",
    val currentLatitude: Double?,
    val currentLongitude: Double?,
    val speed: Double = 0.0,
    val heading: Double = 0.0,
    val accuracy: Double = 0.0,
    val lastLocationUpdate: Date?,
    val isLocationStale: Boolean = false,
    val priority: String = "Medium",
    val estimatedArrival: String = "Calculating...",
    val progressPercentage: Int = 50,
    val tripStartedAt: Date? = null,
    val destinationLatitude: Double? = null,
    val destinationLongitude: Double? = null
)
