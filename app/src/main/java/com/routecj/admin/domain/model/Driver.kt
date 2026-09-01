package com.routecj.admin.domain.model

import java.util.Date

/**
 * Domain model for Driver.
 * Represents a driver in the business logic layer.
 */
data class Driver(
    val id: String = "",
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val role: String = "DRIVER",
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
    val speed: Double = 0.0,
    val heading: Double = 0.0,
    val accuracy: Double = 0.0,
    val address: String = "",
    val isActive: Boolean = true,
    val joinedDate: Date = Date(),
    val lastActive: Date = Date(),
    val createdAt: Date = Date()
)

/**
 * Driver Status enum.
 */
enum class DriverStatus {
    AVAILABLE,
    ON_DUTY,
    BUSY,
    OFF_DUTY,
    INACTIVE,
    SUSPENDED
}

