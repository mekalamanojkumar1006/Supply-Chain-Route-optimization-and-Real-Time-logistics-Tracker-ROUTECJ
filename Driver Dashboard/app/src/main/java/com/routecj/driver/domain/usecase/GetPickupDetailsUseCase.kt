package com.routecj.driver.domain.usecase

import com.routecj.driver.core.util.Result
import com.routecj.driver.domain.model.BookedPickup
import com.routecj.driver.domain.repository.OrderRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * UseCase to observe a single Pickup's live details and verify Driver ownership.
 */
class GetPickupDetailsUseCase(
    private val orderRepository: OrderRepository
) {

    operator fun invoke(orderId: String, currentDriverId: String): Flow<Result<BookedPickup>> {
        return orderRepository.observeOrderById(orderId).map { result ->
            when (result) {
                is Result.Success -> {
                    val order = result.data
                    val assignedDriver = order.assignedDriverId ?: order.driverId

                    if (assignedDriver != currentDriverId) {
                        Result.Error("PICKUP ACCESS DENIED: You are not authorized for this pickup.")
                    } else {
                        Result.Success(
                            BookedPickup(
                                id = order.id,
                                orderNumber = order.orderNumber.ifBlank { "ORD-${order.id.takeLast(6)}" },
                                customerName = order.customerName.ifBlank { "Customer" },
                                customerPhone = order.customerPhone,
                                pickupAddress = order.pickupAddress.ifBlank { order.pickupLocation.ifBlank { "Customer Location" } },
                                pickupPincode = order.pickupPincode,
                                deliveryAddress = order.deliveryAddress.ifBlank { order.deliveryLocation },
                                scheduledDate = order.estimatedDeliveryDate,
                                scheduledSlot = order.pickupSlot.ifBlank { order.estimatedTime.ifBlank { "Scheduled Pickup" } },
                                status = order.status.name,
                                itemName = order.itemName.ifBlank { "Freight Parcel" },
                                quantity = if (order.quantity > 0) order.quantity else 1,
                                weight = order.weight,
                                isFragile = order.isFragile,
                                specialInstructions = order.specialInstructions,
                                driverArrived = order.driverArrived,
                                driverArrivedAt = order.driverArrivedAt,
                                otpVerified = order.otpVerified,
                                otpVerifiedAt = order.otpVerifiedAt,
                                createdAt = order.createdAt,
                                totalAmount = order.totalAmount
                            )
                        )
                    }
                }
                is Result.Error -> Result.Error(result.message, result.throwable)
                is Result.Loading -> Result.Loading
            }
        }
    }
}
