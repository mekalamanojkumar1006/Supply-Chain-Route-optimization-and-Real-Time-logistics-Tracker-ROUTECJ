package com.routecj.admin.domain.model

import java.util.Date

/**
 * Supported report periods.
 */
enum class ReportPeriod {
    TODAY,
    LAST_7_DAYS,
    LAST_30_DAYS,
    THIS_MONTH,
    CUSTOM
}

/**
 * Aggregated metrics for Orders and Order Status Distribution.
 */
data class OrdersReport(
    val total: Int = 0,
    val pending: Int = 0,
    val assigned: Int = 0,
    val pickedUp: Int = 0,
    val pendingGodownReview: Int = 0,
    val qrGenerated: Int = 0,
    val readyForDispatch: Int = 0,
    val dispatched: Int = 0,
    val inTransit: Int = 0,
    val delivered: Int = 0,
    val cancelled: Int = 0,
    val failed: Int = 0,
    val activeTrips: Int = 0,
    val avgDeliveryTimeHours: Double = 0.0,
    val ordersByDay: List<Pair<String, Int>> = emptyList(),
    val statusBreakdown: List<Pair<OrderStatus, Int>> = emptyList(),
    val ordersByPriority: Map<String, Int> = emptyMap(),
    val ordersList: List<Order> = emptyList()
)

/**
 * Delivery Performance Metrics.
 */
data class DeliveryPerformanceReport(
    val totalDeliveries: Int = 0,
    val deliveredToday: Int = 0,
    val deliveredThisWeek: Int = 0,
    val deliveredThisMonth: Int = 0,
    val deliverySuccessRate: Double = 0.0,
    val failedDeliveries: Int = 0,
    val cancelledDeliveries: Int = 0,
    val avgDeliveryDurationMinutes: Double = 0.0,
    val deliveryTrendByDay: List<Pair<String, Int>> = emptyList()
)

/**
 * Aggregated metrics for Driver Performance.
 */
data class DriverPerformanceReport(
    val totalDrivers: Int = 0,
    val availableCount: Int = 0,
    val onTripCount: Int = 0,
    val busyCount: Int = 0,
    val offDutyCount: Int = 0,
    val activeCount: Int = 0,
    val topPerformers: List<DriverStat> = emptyList()
)

data class DriverStat(
    val driverId: String,
    val name: String,
    val phone: String = "",
    val totalTrips: Int = 0,
    val completed: Int = 0,
    val active: Int = 0,
    val cancelled: Int = 0,
    val completionRate: Double = 0.0,
    val currentStatus: DriverStatus = DriverStatus.AVAILABLE
)

/**
 * Aggregated metrics for Vehicle Utilization.
 */
data class VehicleUtilizationReport(
    val totalVehicles: Int = 0,
    val availableCount: Int = 0,
    val assignedCount: Int = 0,
    val inTransitCount: Int = 0,
    val maintenanceCount: Int = 0,
    val inactiveCount: Int = 0,
    val utilizationStats: List<VehicleStat> = emptyList()
)

data class VehicleStat(
    val vehicleId: String = "",
    val registrationNumber: String,
    val vehicleType: String = "",
    val totalTrips: Int = 0,
    val completedTrips: Int = 0,
    val activeTrips: Int = 0,
    val utilizationRate: Double = 0.0,
    val currentStatus: VehicleStatus = VehicleStatus.AVAILABLE
)

/**
 * Aggregated metrics for Godowns.
 */
data class GodownReport(
    val totalGodowns: Int = 0,
    val activeCount: Int = 0,
    val totalCapacity: Double = 0.0,
    val currentStock: Double = 0.0,
    val availableCapacity: Double = 0.0,
    val avgOccupancy: Double = 0.0,
    val incomingParcels: Int = 0,
    val pendingReview: Int = 0,
    val readyForDispatch: Int = 0,
    val dispatched: Int = 0,
    val criticalGodowns: Int = 0
)

