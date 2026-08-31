package com.routecj.admin.domain.repository

import com.routecj.admin.core.util.Result
import com.routecj.admin.domain.model.Dispatch
import com.routecj.admin.domain.model.DispatchStatus
import kotlinx.coroutines.flow.Flow

interface DispatchRepository {
    suspend fun getAllDispatches(): Flow<Result<List<Dispatch>>>
    suspend fun getDispatchById(id: String): Result<Dispatch>
    suspend fun createDispatch(dispatch: Dispatch): Result<Unit>
    suspend fun updateDispatch(dispatch: Dispatch): Result<Unit>
    suspend fun updateDispatchStatus(id: String, status: DispatchStatus): Result<Unit>
    suspend fun assignDriverAndVehicle(dispatchId: String, driverId: String, vehicleId: String): Result<Unit>
    suspend fun createDispatchFromOrder(order: com.routecj.admin.domain.model.Order, driverId: String, vehicleId: String): Result<Unit>
}
