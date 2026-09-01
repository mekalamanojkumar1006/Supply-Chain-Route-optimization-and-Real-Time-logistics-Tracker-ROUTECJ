package com.routecj.driver.domain.usecase

import com.routecj.driver.core.util.Result
import com.routecj.driver.domain.repository.OrderRepository

/**
 * UseCase for the Driver to explicitly mark arrival at the customer pickup location.
 * Validates driver authorization and updates driverArrived = true with a server timestamp.
 */
class MarkDriverArrivedUseCase(
    private val orderRepository: OrderRepository
) {

    suspend operator fun invoke(orderId: String, driverId: String): Result<Unit> {
        if (orderId.isBlank() || driverId.isBlank()) {
            return Result.Error("Invalid order or driver identification")
        }
        return orderRepository.markDriverArrived(orderId, driverId)
    }
}
