package com.routecj.driver.domain.model

import java.util.Date

/**
 * Consolidated model for an assigned delivery / trip item for the driver dashboard.
 */
data class DriverAssignment(
    val id: String,
    val orderId: String,
    val orderNumber: String,
    val customerName: String,
    val customerPhone: String = "",
    val pickupLocation: String,
    val deliveryLocation: String,
    val status: String,
    val priority: String = "Medium",
    val vehicleId: String? = null,
    val vehicleRegistration: String? = null,
    val scheduledDate: Date? = null,
    val createdAt: Date = Date(),
    val totalAmount: Double = 0.0,
    val isDispatchRecord: Boolean = false
)

/**
 * Driver metrics summary calculated from real assigned records.
 */
data class DriverSummaryMetrics(
    val totalAssigned: Int = 0,
    val activeTrips: Int = 0,
    val completedDeliveries: Int = 0,
    val pendingDeliveries: Int = 0
)
