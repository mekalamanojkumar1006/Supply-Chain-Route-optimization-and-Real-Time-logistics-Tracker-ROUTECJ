package com.routecj.driver.domain.model

import java.util.Date

/**
 * Domain model representing a Dispatch operation.
 * Matches existing RouteCJ Firebase 'dispatches' collection schema.
 */
data class Dispatch(
    val id: String = "",
    val orderId: String = "",
    val orderNumber: String = "",
    val customerName: String = "",
    val pickupLocation: String = "",
    val deliveryLocation: String = "",
    val driverId: String? = null,
    val driverName: String? = null,
    val vehicleId: String? = null,
    val vehicleRegistration: String? = null,
    val status: DispatchStatus = DispatchStatus.PENDING,
    val priority: String = "Medium",
    val estimatedDelivery: Date? = null,
    val createdAt: Date = Date(),
    val updatedAt: Date = Date(),
    val remarks: String? = null
)

/**
 * Dispatch Status enum.
 * Matches existing backend status values exactly.
 */
enum class DispatchStatus {
    PENDING,
    ASSIGNED,
    DISPATCH_CONFIRMED,
    TRIP_STARTED,
    IN_TRANSIT,
    DELIVERED,
    CANCELLED
}
