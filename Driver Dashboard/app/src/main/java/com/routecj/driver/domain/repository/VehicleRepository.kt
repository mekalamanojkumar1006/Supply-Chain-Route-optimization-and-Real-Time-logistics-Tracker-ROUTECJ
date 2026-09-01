package com.routecj.driver.domain.repository

import com.routecj.driver.core.util.Result
import com.routecj.driver.domain.model.Vehicle
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for Vehicle assigned to the Driver.
 */
interface VehicleRepository {
    suspend fun getVehicleById(vehicleId: String): Result<Vehicle>
    fun observeVehicleById(vehicleId: String): Flow<Result<Vehicle>>
}
