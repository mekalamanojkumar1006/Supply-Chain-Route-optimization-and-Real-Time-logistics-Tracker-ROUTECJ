package com.routecj.driver.domain.model

import java.util.Date

/**
 * Lightweight domain model representing a historical trip item for the driver.
 * Reuses existing backend fields from dispatches and orders collections.
 */
data class DriverTripHistoryItem(
    val id: String = "",
    val orderId: String = "",
    val orderNumber: String = "",
    val customerName: String = "",
    val pickupAddress: String = "",
    val deliveryAddress: String = "",
    val status: String = "",
    val priority: String = "Medium",
    val vehicleId: String? = null,
    val vehicleRegistration: String? = null,
    val createdAt: Date = Date(),
    val completedAt: Date? = null,
    val totalAmount: Double = 0.0,
    val isDispatchRecord: Boolean = true
)

enum class TripHistoryFilter {
    ALL,
    COMPLETED,
    CANCELLED
}
