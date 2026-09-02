package com.routecj.driver.domain.repository

import com.routecj.driver.core.util.Result
import com.routecj.driver.domain.model.Driver
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for Driver profile and status operations.
 */
interface DriverRepository {
    suspend fun getDriverById(driverId: String): Result<Driver>
    fun observeDriverById(driverId: String): Flow<Result<Driver>>
    suspend fun updateDriverLocation(driverId: String, latitude: Double, longitude: Double): Result<Unit>
    suspend fun updateDriverStatus(driverId: String, status: String): Result<Unit>
}
