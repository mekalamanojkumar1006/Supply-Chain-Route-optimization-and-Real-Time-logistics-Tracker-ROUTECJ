package com.routecj.admin.domain.repository

import com.routecj.admin.core.util.Result
import com.routecj.admin.domain.model.Vehicle
import com.routecj.admin.domain.model.VehicleLog
import kotlinx.coroutines.flow.Flow

/**
 * Vehicle Repository Interface.
 * Defines the contract for fetching and managing vehicles.
 */
interface VehicleRepository {
    /**
     * Get all vehicles reactively (real-time stream).
     */
    suspend fun getAllVehicles(): Flow<Result<List<Vehicle>>>

    /**
     * Get a specific vehicle by its ID.
     */
    suspend fun getVehicleById(vehicleId: String): Result<Vehicle>

    /**
     * Create a new vehicle.
     */
    suspend fun createVehicle(vehicle: Vehicle): Result<Vehicle>

    /**
     * Update an existing vehicle.
     */
    suspend fun updateVehicle(vehicle: Vehicle): Result<Vehicle>

    /**
     * Delete a vehicle by its ID.
     */
    suspend fun deleteVehicle(vehicleId: String): Result<Boolean>

    /**
     * Get logs for a specific vehicle by its ID.
     */
    suspend fun getVehicleLogs(vehicleId: String): Flow<Result<List<VehicleLog>>>

    /**
     * Upload vehicle image and return its URL.
     */
    suspend fun uploadVehicleImage(vehicleId: String, imageUri: android.net.Uri): Result<String>
}
