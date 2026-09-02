package com.routecj.driver.domain.usecase

import com.routecj.driver.core.util.Result
import com.routecj.driver.domain.repository.DispatchRepository
import com.routecj.driver.domain.repository.OrderRepository

/**
 * UseCase to start an assigned trip with atomic synchronization across
 * Dispatch, Order, Driver, and Vehicle.
 */
class StartTripUseCase(
    private val dispatchRepository: DispatchRepository,
    private val orderRepository: OrderRepository
) {

    suspend operator fun invoke(
        tripId: String,
        driverId: String,
        isDispatchRecord: Boolean
    ): Result<Unit> {
        if (driverId.isBlank()) {
            return Result.Error("Driver session invalid")
        }

        return if (isDispatchRecord) {
            dispatchRepository.startTrip(tripId, driverId)
        } else {
            orderRepository.startOrderTrip(tripId, driverId)
        }
    }
}
