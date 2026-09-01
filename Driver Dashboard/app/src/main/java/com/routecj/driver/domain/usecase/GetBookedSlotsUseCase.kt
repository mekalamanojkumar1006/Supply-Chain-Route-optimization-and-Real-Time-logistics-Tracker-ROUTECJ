package com.routecj.driver.domain.usecase

import com.routecj.driver.core.util.Result
import com.routecj.driver.domain.model.BookedPickup
import com.routecj.driver.domain.repository.OrderRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * UseCase to observe assigned pickup bookings for the authenticated Driver.
 */
class GetBookedSlotsUseCase(
    private val orderRepository: OrderRepository
) {

    operator fun invoke(driverId: String): Flow<Result<List<BookedPickup>>> {
        return orderRepository.observeBookedPickups(driverId).map { result ->
            when (result) {
                is Result.Success -> {
                    val pickups = result.data.map { order ->
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
                    }.sortedByDescending { it.createdAt }

                    Result.Success(pickups)
                }
                is Result.Error -> Result.Error(result.message, result.throwable)
                is Result.Loading -> Result.Loading
            }
        }
    }
}
