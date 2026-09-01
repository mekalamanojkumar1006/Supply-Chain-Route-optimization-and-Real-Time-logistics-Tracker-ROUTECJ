package com.routecj.driver.domain.usecase

import com.routecj.driver.core.util.Result
import com.routecj.driver.domain.repository.DispatchRepository
import com.routecj.driver.domain.repository.OrderRepository

class CompleteTripUseCase(
    private val dispatchRepository: DispatchRepository,
    private val orderRepository: OrderRepository
) {
    suspend operator fun invoke(tripId: String, driverId: String, isDispatchRecord: Boolean): Result<Unit> {
        if (driverId.isBlank()) return Result.Error("Driver session invalid")
        if (tripId.isBlank()) return Result.Error("Invalid trip identifier")

        return if (isDispatchRecord) {
            dispatchRepository.completeTrip(tripId, driverId)
        } else {
            orderRepository.completeOrderTrip(tripId, driverId)
        }
    }
}
