package com.routecj.driver.domain.usecase

import com.routecj.driver.core.util.Result
import com.routecj.driver.domain.model.*
import com.routecj.driver.domain.repository.DispatchRepository
import com.routecj.driver.domain.repository.DriverRepository
import com.routecj.driver.domain.repository.OrderRepository
import com.routecj.driver.domain.repository.VehicleRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

/**
 * Composite data representing the full live Driver Home state,
 * including assigned booked pickups count.
 */
data class DriverHomeData(
    val driver: Driver,
    val todayAssignment: DriverAssignment?,
    val nextAssignment: DriverAssignment?,
    val allAssignments: List<DriverAssignment>,
    val summary: DriverSummaryMetrics,
    val bookedPickupsCount: Int = 0,
    val vehicle: Vehicle?,
    val hasAssignedVehicle: Boolean = false
)

/**
 * UseCase to observe live consolidated Driver Home dashboard data.
 */
class GetDriverHomeDataUseCase(
    private val driverRepository: DriverRepository,
    private val orderRepository: OrderRepository,
    private val dispatchRepository: DispatchRepository,
    private val vehicleRepository: VehicleRepository
) {

    operator fun invoke(driverId: String, initialDriver: Driver): Flow<Result<DriverHomeData>> {
        val driverFlow = driverRepository.observeDriverById(driverId)
        val ordersFlow = orderRepository.observeAssignedOrders(driverId)
        val dispatchesFlow = dispatchRepository.observeAssignedDispatches(driverId)
        val bookedPickupsFlow = orderRepository.observeBookedPickups(driverId)

        return combine(driverFlow, ordersFlow, dispatchesFlow, bookedPickupsFlow) { driverRes, ordersRes, dispatchesRes, pickupsRes ->
            try {
                val currentDriver = if (driverRes is Result.Success) driverRes.data else initialDriver

                val ordersList = if (ordersRes is Result.Success) ordersRes.data else emptyList()
                val dispatchesList = if (dispatchesRes is Result.Success) dispatchesRes.data else emptyList()
                val bookedPickupsList = if (pickupsRes is Result.Success) pickupsRes.data else emptyList()

                // Map Dispatches to DriverAssignment
                val dispatchAssignments = dispatchesList.map { d ->
                    DriverAssignment(
                        id = d.id,
                        orderId = d.orderId,
                        orderNumber = d.orderNumber.ifBlank { "DSP-${d.id.takeLast(6)}" },
                        customerName = d.customerName.ifBlank { "Customer" },
                        pickupLocation = d.pickupLocation.ifBlank { "Origin Location" },
                        deliveryLocation = d.deliveryLocation.ifBlank { "Destination Location" },
                        status = d.status.name,
                        priority = d.priority,
                        vehicleId = d.vehicleId,
                        vehicleRegistration = d.vehicleRegistration,
                        scheduledDate = d.estimatedDelivery,
                        createdAt = d.createdAt,
                        isDispatchRecord = true
                    )
                }

                // Map Orders to DriverAssignment (avoiding duplicate order IDs if already in dispatches)
                val existingOrderIds = dispatchesList.map { it.orderId }.toSet()
                val orderAssignments = ordersList.filter { it.id !in existingOrderIds }.map { o ->
                    DriverAssignment(
                        id = o.id,
                        orderId = o.id,
                        orderNumber = o.orderNumber.ifBlank { "ORD-${o.id.takeLast(6)}" },
                        customerName = o.customerName.ifBlank { "Customer" },
                        customerPhone = o.customerPhone,
                        pickupLocation = o.pickupAddress.ifBlank { o.pickupLocation.ifBlank { "Origin Location" } },
                        deliveryLocation = o.deliveryAddress.ifBlank { o.deliveryLocation.ifBlank { "Destination Location" } },
                        status = o.status.name,
                        priority = o.priority,
                        vehicleId = o.assignedVehicleId ?: o.vehicleId,
                        vehicleRegistration = o.vehicleRegistration,
                        scheduledDate = o.estimatedDeliveryDate,
                        createdAt = o.createdAt,
                        totalAmount = o.totalAmount,
                        isDispatchRecord = false
                    )
                }

                val allAssignments = (dispatchAssignments + orderAssignments)
                    .sortedByDescending { it.createdAt }

                val activeAssignments = allAssignments.filter { assignment ->
                    val s = assignment.status.uppercase()
                    s !in listOf("DELIVERED", "CANCELLED", "FAILED")
                }

                val primaryTodayTrip = activeAssignments.firstOrNull { it.status.uppercase() in listOf("TRIP_STARTED", "IN_TRANSIT") }
                    ?: activeAssignments.firstOrNull()

                val nextAssignment = if (primaryTodayTrip != null) {
                    activeAssignments.firstOrNull { it.id != primaryTodayTrip.id }
                } else null

                val completedCount = allAssignments.count { it.status.uppercase() == "DELIVERED" } + currentDriver.completedDeliveries
                val activeCount = activeAssignments.size
                val pendingCount = allAssignments.count { it.status.uppercase() in listOf("PENDING", "ASSIGNED") }

                val summary = DriverSummaryMetrics(
                    totalAssigned = allAssignments.size,
                    activeTrips = activeCount,
                    completedDeliveries = completedCount,
                    pendingDeliveries = pendingCount
                )

                // Define the vehicle ID to observe using Trip Details logic as primary source of truth,
                // fallback to the driver's assignedVehicleId
                val vehicleId = primaryTodayTrip?.vehicleId?.takeIf { it.isNotBlank() }
                    ?: primaryTodayTrip?.vehicleRegistration?.takeIf { it.isNotBlank() }
                    ?: currentDriver.assignedVehicleId?.takeIf { it.isNotBlank() }
                    ?: currentDriver.assignedVehicle?.takeIf { it.isNotBlank() }

                val partialData = DriverHomeData(
                    driver = currentDriver,
                    todayAssignment = primaryTodayTrip,
                    nextAssignment = nextAssignment,
                    allAssignments = allAssignments,
                    summary = summary,
                    bookedPickupsCount = bookedPickupsList.size,
                    vehicle = null, // Will be populated in flatMapLatest
                    hasAssignedVehicle = !vehicleId.isNullOrBlank()
                )
                
                Result.Success(Pair(partialData, vehicleId))
            } catch (e: Exception) {
                Result.Error("Failed to consolidate driver dashboard data: ${e.message}", e)
            }
        }.flatMapLatest { result ->
            when (result) {
                is Result.Success -> {
                    val (partialData, vehicleId) = result.data
                    if (!vehicleId.isNullOrBlank()) {
                        vehicleRepository.observeVehicleById(vehicleId).map { vRes ->
                            val vehicle = if (vRes is Result.Success) vRes.data else null
                            Result.Success(partialData.copy(vehicle = vehicle))
                        }
                    } else {
                        flowOf(Result.Success(partialData))
                    }
                }
                is Result.Error -> flowOf(Result.Error(result.message, result.throwable))
                is Result.Loading -> flowOf(Result.Loading)
            }
        }
    }
}

