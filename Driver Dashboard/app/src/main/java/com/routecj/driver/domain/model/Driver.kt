package com.routecj.driver.domain.model

import java.util.Date

/**
 * Domain model for Driver.
 * Exactly matches the existing RouteCJ Firebase 'drivers' collection schema.
 */
data class Driver(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val licenseNumber: String = "",
    val licenseExpiryDate: Date = Date(),
    val status: DriverStatus = DriverStatus.AVAILABLE,
    val assignedVehicle: String? = null,
    val assignedVehicleId: String? = null,
    val rating: Double = 5.0,
    val totalDeliveries: Int = 0,
    val completedDeliveries: Int = 0,
    val profileImage: String? = null,
    val currentLatitude: Double = 0.0,
    val currentLongitude: Double = 0.0,
    val address: String = "",
    val joinedDate: Date = Date(),
    val lastActive: Date = Date(),
    val createdAt: Date = Date()
)

/**
 * Driver Status enum.
 * Matches existing backend status values exactly.
 */
enum class DriverStatus {
    AVAILABLE,
    ON_DUTY,
    BUSY,
    OFF_DUTY,
    INACTIVE
}
