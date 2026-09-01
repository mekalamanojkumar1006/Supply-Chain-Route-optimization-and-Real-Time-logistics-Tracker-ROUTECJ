package com.routecj.driver.domain.repository

import com.routecj.driver.core.util.Result
import com.routecj.driver.domain.model.Dispatch
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for Dispatch operations assigned to the Driver.
 */
interface DispatchRepository {
    suspend fun getDispatchById(dispatchId: String): Result<Dispatch>
    fun observeDispatchById(dispatchId: String): Flow<Result<Dispatch>>
    fun observeAssignedDispatches(driverId: String): Flow<Result<List<Dispatch>>>
    suspend fun startTrip(dispatchId: String, driverId: String): Result<Unit>
    suspend fun completeTrip(dispatchId: String, driverId: String): Result<Unit>
}
