package com.routecj.admin.presentation.reports

import androidx.lifecycle.viewModelScope
import com.routecj.admin.core.presentation.BaseViewModel
import com.routecj.admin.core.util.Result
import com.routecj.admin.domain.model.*
import com.routecj.admin.domain.repository.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ReportsViewModel @Inject constructor(
    private val orderRepository: OrderRepository,
    private val driverRepository: DriverRepository,
    private val vehicleRepository: VehicleRepository,
    private val dispatchRepository: DispatchRepository,
    private val godownRepository: GodownRepository,
    private val sessionManager: com.routecj.admin.core.security.SessionManager
) : BaseViewModel() {

    val currentAdmin = sessionManager.currentAdmin
    
    private val _selectedPeriod = MutableStateFlow(ReportPeriod.LAST_7_DAYS)
    val selectedPeriod = _selectedPeriod.asStateFlow()

    private val _customDateRange = MutableStateFlow<Pair<Date, Date>?>(null)
    val customDateRange = _customDateRange.asStateFlow()

    // 1. Orders & Status Breakdown Report State
    val ordersReport: StateFlow<Result<OrdersReport>> = combine(
        selectedPeriod,
        _customDateRange,
        flow { emit(orderRepository.getAllOrders()) }.flatMapLatest { it }
    ) { period, range, result ->
        if (result is Result.Success) {
            val allOrders = result.data
            val filtered = filterByDate(allOrders, period, range) { it.createdAt }
            
            val total = filtered.size
            val pending = filtered.count { it.status == OrderStatus.PENDING }
            val assigned = filtered.count { it.status == OrderStatus.ASSIGNED }
            val pickedUp = filtered.count { it.status == OrderStatus.PICKED_UP }
            val pendingGodownReview = filtered.count { it.status == OrderStatus.PENDING_GODOWN_REVIEW }
            val qrGenerated = filtered.count { it.status == OrderStatus.QR_GENERATED }
            val readyForDispatch = filtered.count { it.status == OrderStatus.READY_FOR_DISPATCH }
            val dispatched = filtered.count { it.status == OrderStatus.DISPATCHED }
            val inTransit = filtered.count { it.status == OrderStatus.IN_TRANSIT }
            val delivered = filtered.count { it.status == OrderStatus.DELIVERED }
            val cancelled = filtered.count { it.status == OrderStatus.CANCELLED }
            val failed = filtered.count { it.status == OrderStatus.FAILED }
            val activeTrips = assigned + pickedUp + pendingGodownReview + qrGenerated + readyForDispatch + dispatched + inTransit

            // Calculate actual average delivery time from createdAt to deliveredAt where both exist
            val deliveredOrdersWithTimes = filtered.filter { it.status == OrderStatus.DELIVERED && it.deliveredAt != null }
            val avgDeliveryTimeHours = if (deliveredOrdersWithTimes.isNotEmpty()) {
                val totalDurationMs = deliveredOrdersWithTimes.sumOf { 
                    (it.deliveredAt!!.time - it.createdAt.time).coerceAtLeast(0L) 
                }
                (totalDurationMs.toDouble() / (deliveredOrdersWithTimes.size * 3600000.0))
            } else 0.0

            // Group by day for chart
            val df = SimpleDateFormat("dd/MM", Locale.getDefault())
            val byDay = filtered.groupBy { df.format(it.createdAt) }
                .mapValues { it.value.size }
                .toList()
                .sortedBy { it.first }

            val statusBreakdown = OrderStatus.entries.map { status ->
                status to filtered.count { it.status == status }
            }

            Result.Success(OrdersReport(
                total = total,
                pending = pending,
                assigned = assigned,
                pickedUp = pickedUp,
                pendingGodownReview = pendingGodownReview,
                qrGenerated = qrGenerated,
                readyForDispatch = readyForDispatch,
                dispatched = dispatched,
                inTransit = inTransit,
                delivered = delivered,
                cancelled = cancelled,
                failed = failed,
                activeTrips = activeTrips,
                avgDeliveryTimeHours = avgDeliveryTimeHours,
                ordersByDay = byDay,
                statusBreakdown = statusBreakdown,
                ordersByPriority = filtered.groupBy { it.priority }.mapValues { it.value.size },
                ordersList = filtered
            ))
        } else Result.Loading()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Result.Loading())

    // 2. Delivery Performance Analytics
    val deliveryPerformanceReport: StateFlow<Result<DeliveryPerformanceReport>> = combine(
        selectedPeriod,
        _customDateRange,
        flow { emit(orderRepository.getAllOrders()) }.flatMapLatest { it }
    ) { period, range, result ->
        if (result is Result.Success) {
            val allOrders = result.data
            val filtered = filterByDate(allOrders, period, range) { it.createdAt }

            val cal = Calendar.getInstance()
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            val todayStart = cal.timeInMillis

            cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
            val weekStart = cal.timeInMillis

            cal.set(Calendar.DAY_OF_MONTH, 1)
            val monthStart = cal.timeInMillis

            val deliveredOrders = filtered.filter { it.status == OrderStatus.DELIVERED }
            val deliveredToday = allOrders.count { it.status == OrderStatus.DELIVERED && (it.deliveredAt?.time ?: it.updatedAt.time) >= todayStart }
            val deliveredThisWeek = allOrders.count { it.status == OrderStatus.DELIVERED && (it.deliveredAt?.time ?: it.updatedAt.time) >= weekStart }
            val deliveredThisMonth = allOrders.count { it.status == OrderStatus.DELIVERED && (it.deliveredAt?.time ?: it.updatedAt.time) >= monthStart }

            val totalCompleted = deliveredOrders.size
            val failedCount = filtered.count { it.status == OrderStatus.FAILED }
            val cancelledCount = filtered.count { it.status == OrderStatus.CANCELLED }
            val totalTerminal = totalCompleted + failedCount + cancelledCount

            val successRate = if (totalTerminal > 0) (totalCompleted.toDouble() / totalTerminal) * 100.0 else if (filtered.isNotEmpty()) (totalCompleted.toDouble() / filtered.size) * 100.0 else 0.0

            // Delivery trend by day
            val df = SimpleDateFormat("dd/MM", Locale.getDefault())
            val deliveryTrendByDay = deliveredOrders.groupBy { 
                df.format(it.deliveredAt ?: it.updatedAt) 
            }.mapValues { it.value.size }.toList().sortedBy { it.first }

            // Avg delivery duration in minutes
            val avgDurationMinutes = if (deliveredOrders.isNotEmpty()) {
                val totalMinutes = deliveredOrders.mapNotNull { 
                    if (it.deliveredAt != null) (it.deliveredAt.time - it.createdAt.time) / 60000.0 else null 
                }
                if (totalMinutes.isNotEmpty()) totalMinutes.average() else 0.0
            } else 0.0

            Result.Success(DeliveryPerformanceReport(
                totalDeliveries = totalCompleted,
                deliveredToday = deliveredToday,
                deliveredThisWeek = deliveredThisWeek,
                deliveredThisMonth = deliveredThisMonth,
                deliverySuccessRate = successRate,
                failedDeliveries = failedCount,
                cancelledDeliveries = cancelledCount,
                avgDeliveryDurationMinutes = avgDurationMinutes,
                deliveryTrendByDay = deliveryTrendByDay
            ))
        } else Result.Loading()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Result.Loading())

    // 3. Driver Performance Report State
    val driverReport: StateFlow<Result<DriverPerformanceReport>> = combine(
        selectedPeriod,
        _customDateRange,
        flow { emit(driverRepository.getAllDrivers()) }.flatMapLatest { it },
        flow { emit(orderRepository.getAllOrders()) }.flatMapLatest { it }
    ) { period, range, dResult, oResult ->
        if (dResult is Result.Success && oResult is Result.Success) {
            val drivers = dResult.data
            val orders = oResult.data
            val filteredOrders = filterByDate(orders, period, range) { it.createdAt }

            val stats = drivers.map { driver ->
                val driverOrders = filteredOrders.filter { it.assignedDriverId == driver.id || it.driverId == driver.id }
                val completed = driverOrders.count { it.status == OrderStatus.DELIVERED }
                val active = driverOrders.count { 
                    it.status == OrderStatus.ASSIGNED || 
                    it.status == OrderStatus.PICKED_UP || 
                    it.status == OrderStatus.DISPATCHED || 
                    it.status == OrderStatus.IN_TRANSIT 
                }
                val cancelled = driverOrders.count { it.status == OrderStatus.CANCELLED || it.status == OrderStatus.FAILED }
                
                val completionRate = if (driverOrders.isNotEmpty()) {
                    (completed.toDouble() / driverOrders.size) * 100.0
                } else 0.0

                DriverStat(
                    driverId = driver.id,
                    name = driver.name,
                    phone = driver.phone,
                    totalTrips = driverOrders.size,
                    completed = completed,
                    active = active,
                    cancelled = cancelled,
                    completionRate = completionRate,
                    currentStatus = driver.status
                )
            }.sortedWith(compareByDescending<DriverStat> { it.completed }.thenByDescending { it.totalTrips })

            Result.Success(DriverPerformanceReport(
                totalDrivers = drivers.size,
                availableCount = drivers.count { it.status == DriverStatus.AVAILABLE },
                onTripCount = drivers.count { it.status == DriverStatus.ON_DUTY },
                busyCount = drivers.count { it.status == DriverStatus.BUSY },
                offDutyCount = drivers.count { it.status == DriverStatus.OFF_DUTY || it.status == DriverStatus.INACTIVE },
                activeCount = drivers.count { it.status == DriverStatus.ON_DUTY || it.status == DriverStatus.BUSY },
                topPerformers = stats
            ))
        } else Result.Loading()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Result.Loading())

    // 4. Vehicle Utilization Report State
    val vehicleReport: StateFlow<Result<VehicleUtilizationReport>> = combine(
        selectedPeriod,
        _customDateRange,
        flow { emit(vehicleRepository.getAllVehicles()) }.flatMapLatest { it },
        flow { emit(orderRepository.getAllOrders()) }.flatMapLatest { it }
    ) { period, range, vResult, oResult ->
        if (vResult is Result.Success && oResult is Result.Success) {
            val vehicles = vResult.data
            val orders = oResult.data
            val filteredOrders = filterByDate(orders, period, range) { it.createdAt }

            val stats = vehicles.map { vehicle ->
                val vehicleOrders = filteredOrders.filter { it.assignedVehicleId == vehicle.id || it.vehicleId == vehicle.id }
                val completed = vehicleOrders.count { it.status == OrderStatus.DELIVERED }
                val active = vehicleOrders.count { 
                    it.status == OrderStatus.ASSIGNED || 
                    it.status == OrderStatus.DISPATCHED || 
                    it.status == OrderStatus.IN_TRANSIT 
                }
                
                val rate = if (vehicleOrders.isNotEmpty()) {
                    (completed.toDouble() / vehicleOrders.size) * 100.0
                } else 0.0

                VehicleStat(
                    vehicleId = vehicle.id,
                    registrationNumber = vehicle.registrationNumber.ifBlank { vehicle.vehicleNumber },
                    vehicleType = vehicle.vehicleType.name.replace("_", " "),
                    totalTrips = vehicleOrders.size,
                    completedTrips = completed,
                    activeTrips = active,
                    utilizationRate = rate,
                    currentStatus = vehicle.status
                )
            }.sortedWith(compareByDescending<VehicleStat> { it.totalTrips }.thenByDescending { it.completedTrips })

            Result.Success(VehicleUtilizationReport(
                totalVehicles = vehicles.size,
                availableCount = vehicles.count { it.status == VehicleStatus.AVAILABLE },
                assignedCount = vehicles.count { it.status == VehicleStatus.ASSIGNED },
                inTransitCount = vehicles.count { it.status == VehicleStatus.IN_TRANSIT },
                maintenanceCount = vehicles.count { it.status == VehicleStatus.MAINTENANCE },
                inactiveCount = vehicles.count { it.status == VehicleStatus.INACTIVE },
                utilizationStats = stats
            ))
        } else Result.Loading()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Result.Loading())

    // 5. Godown Operations Report State
    val godownReport: StateFlow<Result<GodownReport>> = combine(
        flow { emit(godownRepository.getAllGodowns()) }.flatMapLatest { it },
        flow { emit(orderRepository.getAllOrders()) }.flatMapLatest { it }
    ) { gResult, oResult ->
        if (gResult is Result.Success && oResult is Result.Success) {
            val godowns = gResult.data
            val orders = oResult.data

            val totalCapacity = godowns.sumOf { it.capacity }
            val currentStock = godowns.sumOf { it.currentStock }
            val available = (totalCapacity - currentStock).coerceAtLeast(0.0)

            val incoming = orders.count { it.status == OrderStatus.PICKED_UP || it.status == OrderStatus.PENDING }
            val pendingReview = orders.count { it.status == OrderStatus.PENDING_GODOWN_REVIEW }
            val readyForDisp = orders.count { it.status == OrderStatus.READY_FOR_DISPATCH || it.status == OrderStatus.QR_GENERATED }
            val disp = orders.count { it.status == OrderStatus.DISPATCHED || it.status == OrderStatus.IN_TRANSIT }

            Result.Success(GodownReport(
                totalGodowns = godowns.size,
                activeCount = godowns.count { it.status == GodownStatus.ACTIVE },
                totalCapacity = totalCapacity,
                currentStock = currentStock,
                availableCapacity = available,
                avgOccupancy = if (totalCapacity > 0) (currentStock / totalCapacity) * 100.0 else 0.0,
                incomingParcels = incoming,
                pendingReview = pendingReview,
                readyForDispatch = readyForDisp,
                dispatched = disp,
                criticalGodowns = godowns.count { it.occupancyPercentage > 90 }
            ))
        } else Result.Loading()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Result.Loading())

    fun setPeriod(period: ReportPeriod) {
        _selectedPeriod.value = period
    }

    fun setCustomRange(start: Date, end: Date) {
        _customDateRange.value = start to end
        _selectedPeriod.value = ReportPeriod.CUSTOM
    }

    /**
     * Generates a CSV string for the current Orders & Logistics report.
     */
    fun exportOrdersToCSV(): String {
        val state = ordersReport.value
        if (state !is Result.Success) return ""
        val data = state.data
        
        val sb = StringBuilder()
        sb.append("RouteCJ Logistics Operational Intelligence Report\n")
        sb.append("Generated Period,${selectedPeriod.value.name}\n\n")
        sb.append("METRIC,COUNT\n")
        sb.append("Total Orders,${data.total}\n")
        sb.append("Delivered,${data.delivered}\n")
        sb.append("In Transit,${data.inTransit}\n")
        sb.append("Dispatched,${data.dispatched}\n")
        sb.append("Ready for Dispatch,${data.readyForDispatch}\n")
        sb.append("Pending Review,${data.pendingGodownReview}\n")
        sb.append("Assigned,${data.assigned}\n")
        sb.append("Pending,${data.pending}\n")
        sb.append("Cancelled,${data.cancelled}\n")
        sb.append("Failed,${data.failed}\n\n")
        
        sb.append("Daily Volume Trend\n")
        sb.append("Date,Order Volume\n")
        data.ordersByDay.forEach { (date, count) ->
            sb.append("$date,$count\n")
        }
        sb.append("\n")

        sb.append("Detailed Order Records\n")
        sb.append("Order Number,Customer Name,Customer Phone,Pickup Address,Pickup PIN,Delivery Address,Delivery PIN,Weight (kg),Quantity,Status,Payment Status,Total Amount (INR),Created At\n")

        val df = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        data.ordersList.forEach { order ->
            val num = escapeCsv(order.orderNumber)
            val name = escapeCsv(order.customerName.ifBlank { "Customer" })
            val phone = escapeCsv(order.customerPhone)
            val pickup = escapeCsv(order.pickupAddress.ifBlank { order.pickupLocation })
            val pickupPin = escapeCsv(order.pickupPincode)
            val deliv = escapeCsv(order.deliveryAddress.ifBlank { order.deliveryLocation.ifBlank { order.customerAddress } })
            val delivPin = escapeCsv(order.deliveryPincode)
            val status = escapeCsv(order.status.name)
            val payStatus = escapeCsv(order.paymentStatus)
            val dateStr = escapeCsv(df.format(order.createdAt))

            sb.append("$num,$name,$phone,$pickup,$pickupPin,$deliv,$delivPin,${order.weight},${order.quantity},$status,$payStatus,${order.totalAmount},$dateStr\n")
        }
        
        return sb.toString()
    }

    private fun escapeCsv(value: String): String {
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"${value.replace("\"", "\"\"")}\""
        }
        return value
    }

    private fun <T> filterByDate(
        list: List<T>,
        period: ReportPeriod,
        range: Pair<Date, Date>?,
        dateSelector: (T) -> Date
    ): List<T> {
        val now = Date()
        val calendar = Calendar.getInstance()
        
        val startTime: Long = when (period) {
            ReportPeriod.TODAY -> {
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                calendar.timeInMillis
            }
            ReportPeriod.LAST_7_DAYS -> {
                calendar.add(Calendar.DAY_OF_YEAR, -7)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.timeInMillis
            }
            ReportPeriod.LAST_30_DAYS -> {
                calendar.add(Calendar.DAY_OF_YEAR, -30)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.timeInMillis
            }
            ReportPeriod.THIS_MONTH -> {
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.timeInMillis
            }
            ReportPeriod.CUSTOM -> range?.first?.time ?: 0L
        }

        val endTime: Long = if (period == ReportPeriod.CUSTOM) range?.second?.time ?: Long.MAX_VALUE else now.time

        return list.filter {
            val itemTime = dateSelector(it).time
            itemTime in startTime..endTime
        }
    }
}

