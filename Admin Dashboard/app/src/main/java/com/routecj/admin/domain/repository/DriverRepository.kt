package com.routecj.admin.domain.repository

import com.routecj.admin.core.util.Result
import com.routecj.admin.domain.model.Driver
import kotlinx.coroutines.flow.Flow

/**
 * Driver Repository Interface.
 * Defines the contract for fetching and managing drivers.
 */
interface DriverRepository {
    /**
     * Get all drivers reactively (real-time stream).
     */
    suspend fun getAllDrivers(): Flow<Result<List<Driver>>>

    /**
     * Get driver details by ID.
     */
    suspend fun getDriverById(driverId: String): Result<Driver>

    /**
     * Create a new driver profile.
     */
    suspend fun createDriver(driver: Driver): Result<Driver>

    /**
     * Update driver profile details.
     */
    suspend fun updateDriver(driver: Driver): Result<Driver>

    /**
     * Delete driver profile.
     */
    suspend fun deleteDriver(driverId: String): Result<Boolean>
}
