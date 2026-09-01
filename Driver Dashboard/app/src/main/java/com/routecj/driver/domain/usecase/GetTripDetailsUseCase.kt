package com.routecj.driver.domain.usecase

import com.routecj.driver.core.util.Result
import com.routecj.driver.domain.model.TripDetails
import com.routecj.driver.domain.repository.DispatchRepository
import com.routecj.driver.domain.repository.OrderRepository
import com.routecj.driver.domain.repository.VehicleRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.map

/**
 * UseCase to observe a single Trip's live details.
 * Checks whether the ID is a Dispatch document or an Order document,
 * loads linked Order/Vehicle records, and enforces driver ownership.
 */
class GetTripDetailsUseCase(
    private val dispatchRepository: DispatchRepository,
    private val orderRepository: OrderRepository,
    private val vehicleRepository: VehicleRepository
) {

    operator fun invoke(tripId: String, currentDriverId: String): Flow<Result<TripDetails>> = flow {
        // Try observing as a Dispatch document first
        val dispatchFlow = dispatchRepository.observeDispatchById(tripId).map { dispatchResult ->
            when (dispatchResult) {
                is Result.Success -> {
                    val dispatch = dispatchResult.data
                    // Security / Ownership check
                    if (dispatch.driverId != currentDriverId) {
                        return@map Result.Error("TRIP ACCESS DENIED: You are not authorized to access this trip.")
                    }

                    // Fetch optional order parcel details
                    var itemName = "Freight Parcel"
                    var weight = 0.0
                    var quantity = 1
                    var isFragile = false
                    var specialInstructions = ""
                    var customerPhone = ""

                    var originLat = 0.0
                    var originLng = 0.0
                    var destinationLat = 0.0
                    var destinationLng = 0.0

                    if (dispatch.orderId.isNotBlank()) {
                        when (val orderRes = orderRepository.getOrderById(dispatch.orderId)) {
                            is Result.Success -> {
                                val o = orderRes.data
                                itemName = o.itemName.ifBlank { "Freight Parcel" }
                                weight = o.weight
                                quantity = if (o.quantity > 0) o.quantity else 1
                                isFragile = o.isFragile
                                specialInstructions = o.specialInstructions
                                customerPhone = o.customerPhone
                                originLat = o.origin.latitude
                                originLng = o.origin.longitude
                                destinationLat = o.destination.latitude
                                destinationLng = o.destination.longitude
                            }
                            else -> {}
                        }
                    }

                    // Resolve real vehicle registration from vehicleId if not populated
                    var resolvedVehicleRegistration = dispatch.vehicleRegistration
                    var resolvedVehicleType: String? = null
                    val vehicleDocId = dispatch.vehicleId
                    if (!vehicleDocId.isNullOrBlank()) {
                        when (val vRes = vehicleRepository.getVehicleById(vehicleDocId)) {
                            is Result.Success -> {
                                val v = vRes.data
                                val reg = v.registrationNumber.ifBlank { v.vehicleNumber }
                                if (reg.isNotBlank()) {
                                    resolvedVehicleRegistration = reg
                                }
                                resolvedVehicleType = v.vehicleType.name
                            }
                            else -> {}
                        }
                    }

                    Result.Success(
                        TripDetails(
                            tripId = dispatch.id,
                            orderId = dispatch.orderId,
                            orderNumber = dispatch.orderNumber.ifBlank { "DSP-${dispatch.id.takeLast(6)}" },
                            customerName = dispatch.customerName.ifBlank { "Customer" },
                            customerPhone = customerPhone,
                            pickupAddress = dispatch.pickupLocation.ifBlank { "Pickup Hub" },
                            deliveryAddress = dispatch.deliveryLocation.ifBlank { "Delivery Destination" },
                            status = dispatch.status.name,
                            priority = dispatch.priority,
                            vehicleId = dispatch.vehicleId,
                            vehicleRegistration = resolvedVehicleRegistration,
                            vehicleType = resolvedVehicleType,
                            itemName = itemName,
                            weight = weight,
                            quantity = quantity,
                            isFragile = isFragile,
                            specialInstructions = specialInstructions,
                            driverId = dispatch.driverId ?: currentDriverId,
                            driverName = dispatch.driverName,
                            scheduledDate = dispatch.estimatedDelivery,
                            originLat = originLat,
                            originLng = originLng,
                            destinationLat = destinationLat,
                            destinationLng = destinationLng,
                            createdAt = dispatch.createdAt,
                            isDispatchRecord = true,
                            otpVerified = dispatch.status.name.uppercase() in listOf("DELIVERED", "IN_TRANSIT", "DISPATCHED"),
                            driverArrived = dispatch.status.name.uppercase() != "ASSIGNED"
                        )
                    )
                }
                is Result.Error -> {
                    // Fallback to observing as an Order directly
                    null
                }
                is Result.Loading -> Result.Loading
            }
        }

        // Check if Dispatch stream produces a valid result or if we fallback to Order
        when (val initialDispatch = dispatchRepository.getDispatchById(tripId)) {
            is Result.Success -> {
                emitAll(dispatchFlow.map { it ?: Result.Error("Failed to observe dispatch") })
            }
            else -> {
                // Observe Order stream directly
                emitAll(
                    orderRepository.observeOrderById(tripId).map { orderResult ->
                        when (orderResult) {
                            is Result.Success -> {
                                val o = orderResult.data
                                val assignedDriver = o.assignedDriverId ?: o.driverId
                                if (assignedDriver != currentDriverId) {
                                    Result.Error("TRIP ACCESS DENIED: You are not authorized to access this trip.")
                                } else {
                                    // Resolve vehicle registration from vehicleId if needed
                                    var resolvedVehicleRegistration = o.vehicleRegistration
                                    var resolvedVehicleType = o.vehicleType
                                    val vehicleDocId = o.assignedVehicleId ?: o.vehicleId
                                    if (!vehicleDocId.isNullOrBlank()) {
                                        when (val vRes = vehicleRepository.getVehicleById(vehicleDocId)) {
                                            is Result.Success -> {
                                                val v = vRes.data
                                                val reg = v.registrationNumber.ifBlank { v.vehicleNumber }
                                                if (reg.isNotBlank()) {
                                                    resolvedVehicleRegistration = reg
                                                }
                                                resolvedVehicleType = v.vehicleType.name
                                            }
                                            else -> {}
                                        }
                                    }

                                    Result.Success(
                                        TripDetails(
                                            tripId = o.id,
                                            orderId = o.id,
                                            orderNumber = o.orderNumber.ifBlank { "ORD-${o.id.takeLast(6)}" },
                                            customerName = o.customerName.ifBlank { "Customer" },
                                            customerPhone = o.customerPhone,
                                            pickupAddress = o.pickupAddress.ifBlank { o.pickupLocation.ifBlank { "Pickup Hub" } },
                                            deliveryAddress = o.deliveryAddress.ifBlank { o.deliveryLocation.ifBlank { "Delivery Destination" } },
                                            status = o.status.name,
                                            priority = o.priority,
                                            vehicleId = vehicleDocId,
                                            vehicleRegistration = resolvedVehicleRegistration,
                                            vehicleType = resolvedVehicleType,
                                            itemName = o.itemName.ifBlank { "Freight Parcel" },
                                            weight = o.weight,
                                            quantity = if (o.quantity > 0) o.quantity else 1,
                                            isFragile = o.isFragile,
                                            specialInstructions = o.specialInstructions,
                                            totalAmount = o.totalAmount,
                                            driverId = assignedDriver ?: currentDriverId,
                                            driverName = o.driverName,
                                            scheduledDate = o.estimatedDeliveryDate,
                                            originLat = o.origin.latitude,
                                            originLng = o.origin.longitude,
                                            destinationLat = o.destination.latitude,
                                            destinationLng = o.destination.longitude,
                                            createdAt = o.createdAt,
                                            isDispatchRecord = false,
                                            otpVerified = o.otpVerified,
                                            driverArrived = o.driverArrived
                                        )
                                    )
                                }
                            }
                            is Result.Error -> Result.Error(orderResult.message, orderResult.throwable)
                            is Result.Loading -> Result.Loading
                        }
                    }
                )
            }
        }
    }
}
