package com.routecj.driver.domain.usecase

import com.routecj.driver.core.util.Result
import com.routecj.driver.domain.model.DriverTripHistoryItem
import com.routecj.driver.domain.model.TripHistoryFilter
import com.routecj.driver.domain.repository.DispatchRepository
import com.routecj.driver.domain.repository.OrderRepository
import com.routecj.driver.domain.repository.VehicleRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.util.Date

/**
 * UseCase to observe and filter historical trips for the authenticated driver.
 * Merges dispatches and orders assigned to the driver, resolving vehicle numbers,
 * applying newest-first sorting, and filtering based on backend status values.
 */
class GetDriverTripHistoryUseCase(
    private val dispatchRepository: DispatchRepository,
    private val orderRepository: OrderRepository,
    private val vehicleRepository: VehicleRepository
) {

    operator fun invoke(driverId: String, filter: TripHistoryFilter): Flow<Result<List<DriverTripHistoryItem>>> {
        val dispatchesFlow = dispatchRepository.observeAssignedDispatches(driverId)
        val ordersFlow = orderRepository.observeAssignedOrders(driverId)

        return combine(dispatchesFlow, ordersFlow) { dispatchesRes, ordersRes ->
            try {
                val dispatches = if (dispatchesRes is Result.Success) dispatchesRes.data else emptyList()
                val orders = if (ordersRes is Result.Success) ordersRes.data else emptyList()

                val dispatchItems = dispatches.map { d ->
                    DriverTripHistoryItem(
                        id = d.id,
                        orderId = d.orderId,
                        orderNumber = d.orderNumber.ifBlank { "DSP-${d.id.takeLast(6)}" },
                        customerName = d.customerName.ifBlank { "Customer" },
                        pickupAddress = d.pickupLocation.ifBlank { "Origin Location" },
                        deliveryAddress = d.deliveryLocation.ifBlank { "Destination Location" },
                        status = d.status.name,
                        priority = d.priority,
                        vehicleId = d.vehicleId,
                        vehicleRegistration = d.vehicleRegistration,
                        createdAt = d.createdAt,
                        completedAt = if (d.status.name.uppercase() in listOf("DELIVERED", "CANCELLED")) d.updatedAt else null,
                        isDispatchRecord = true
                    )
                }

                val existingOrderIds = dispatches.map { it.orderId }.toSet()
                val orderItems = orders.filter { it.id !in existingOrderIds }.map { o ->
                    DriverTripHistoryItem(
                        id = o.id,
                        orderId = o.id,
                        orderNumber = o.orderNumber.ifBlank { "ORD-${o.id.takeLast(6)}" },
                        customerName = o.customerName.ifBlank { "Customer" },
                        pickupAddress = o.pickupAddress.ifBlank { o.pickupLocation.ifBlank { "Origin Location" } },
                        deliveryAddress = o.deliveryAddress.ifBlank { o.deliveryLocation.ifBlank { "Destination Location" } },
                        status = o.status.name,
                        priority = o.priority,
                        vehicleId = o.assignedVehicleId ?: o.vehicleId,
                        vehicleRegistration = o.vehicleRegistration,
                        createdAt = o.createdAt,
                        completedAt = o.deliveredAt ?: (if (o.status.name.uppercase() in listOf("DELIVERED", "CANCELLED")) o.updatedAt else null),
                        totalAmount = o.totalAmount,
                        isDispatchRecord = false
                    )
                }

                // Consolidated list sorted by newest first
                val allItems = (dispatchItems + orderItems).sortedWith(
                    compareByDescending<DriverTripHistoryItem> { it.completedAt ?: it.createdAt }
                        .thenByDescending { it.createdAt }
                )

                val filtered = when (filter) {
                    TripHistoryFilter.ALL -> allItems
                    TripHistoryFilter.COMPLETED -> allItems.filter {
                        it.status.uppercase() in listOf("DELIVERED", "COMPLETED")
                    }
                    TripHistoryFilter.CANCELLED -> allItems.filter {
                        it.status.uppercase() in listOf("CANCELLED", "FAILED")
                    }
                }

                Result.Success(filtered)
            } catch (e: Exception) {
                Result.Error("Failed to load trip history: ${e.message}", e)
            }
        }
    }
}
