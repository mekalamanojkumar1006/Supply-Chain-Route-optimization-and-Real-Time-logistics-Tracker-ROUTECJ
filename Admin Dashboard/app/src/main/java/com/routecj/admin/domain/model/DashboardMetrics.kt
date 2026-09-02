package com.routecj.admin.domain.model

/**
 * Domain model representing consolidated metrics/statistics shown on the Dashboard.
 */
data class DashboardMetrics(
    val totalOrders: Int = 0,
    val activeTrips: Int = 0,
    val deliveredOrders: Int = 0,
    val pendingOrders: Int = 0,
    val assignedOrders: Int = 0,
    val pickedUpOrders: Int = 0,
    val cancelledOrders: Int = 0,
    val driverCount: Int = 0,
    val activeDrivers: Int = 0,
    val vehicleCount: Int = 0,
    val availableVehicles: Int = 0,
    val godownCount: Int = 0,
    val activeGodowns: Int = 0,
    val totalAvailableCapacity: Double = 0.0,
    val unreadNotificationsCount: Int = 0,

    // Godown Manager Metrics
    val pendingGodownReview: Int = 0,
    val qrGeneratedCount: Int = 0,
    val receivedCount: Int = 0,
    val readyForDispatchCount: Int = 0,

    // Dispatch Manager Metrics
    val pendingDispatchCount: Int = 0,
    val activeDispatchTrips: Int = 0,
    val availableDriversForDispatch: Int = 0,
    val availableVehiclesForDispatch: Int = 0
)
