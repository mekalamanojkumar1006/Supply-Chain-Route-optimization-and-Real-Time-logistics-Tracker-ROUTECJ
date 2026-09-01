package com.routecj.admin.domain.usecase

import com.routecj.admin.core.util.Result
import com.routecj.admin.domain.model.Order
import com.routecj.admin.domain.repository.DispatchRepository
import javax.inject.Inject

class CreateDispatchFromOrderUseCase @Inject constructor(
    private val repository: DispatchRepository
) {
    suspend operator fun invoke(order: Order, driverId: String, vehicleId: String): Result<Unit> {
        return repository.createDispatchFromOrder(order, driverId, vehicleId)
    }
}
