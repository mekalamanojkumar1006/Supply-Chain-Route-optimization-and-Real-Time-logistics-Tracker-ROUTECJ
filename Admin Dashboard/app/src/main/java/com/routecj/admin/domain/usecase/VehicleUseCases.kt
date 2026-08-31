package com.routecj.admin.domain.usecase

import com.routecj.admin.core.util.Result
import com.routecj.admin.domain.model.Vehicle
import com.routecj.admin.domain.repository.VehicleRepository
import kotlinx.coroutines.flow.Flow
import java.util.Date
import javax.inject.Inject

/**
 * Use case to retrieve and listen to live updates of all vehicles.
 */
class GetVehiclesUseCase @Inject constructor(
    private val vehicleRepository: VehicleRepository
) {
    suspend operator fun invoke(): Flow<Result<List<Vehicle>>> {
        return vehicleRepository.getAllVehicles()
    }
}

/**
 * Use case to create a new vehicle.
 * Enforces all requested validations.
 */
class CreateVehicleUseCase @Inject constructor(
    private val vehicleRepository: VehicleRepository
) {
    suspend operator fun invoke(vehicle: Vehicle): Result<Vehicle> {
        return when {
            vehicle.vehicleNumber.isBlank() -> Result.Error("Vehicle number is required")
            vehicle.brand.isBlank() -> Result.Error("Brand/Make is required")
            vehicle.model.isBlank() -> Result.Error("Model is required")
            vehicle.registrationNumber.isBlank() -> Result.Error("Registration number is required")
            vehicle.capacity <= 0 -> Result.Error("Capacity must be greater than 0")
            vehicle.fuelLevel !in 0.0..100.0 -> Result.Error("Fuel level must be between 0 and 100%")
            vehicle.insuranceExpiry.before(Date()) -> Result.Error("Insurance expiry date must be in the future")
            else -> vehicleRepository.createVehicle(vehicle)
        }
    }
}

/**
 * Use case to update an existing vehicle.
 * Enforces all requested validations.
 */
class UpdateVehicleUseCase @Inject constructor(
    private val vehicleRepository: VehicleRepository
) {
    suspend operator fun invoke(vehicle: Vehicle): Result<Vehicle> {
        return when {
            vehicle.id.isBlank() -> Result.Error("Vehicle ID cannot be empty")
            vehicle.vehicleNumber.isBlank() -> Result.Error("Vehicle number is required")
            vehicle.brand.isBlank() -> Result.Error("Brand/Make is required")
            vehicle.model.isBlank() -> Result.Error("Model is required")
            vehicle.registrationNumber.isBlank() -> Result.Error("Registration number is required")
            vehicle.capacity <= 0 -> Result.Error("Capacity must be greater than 0")
            vehicle.fuelLevel !in 0.0..100.0 -> Result.Error("Fuel level must be between 0 and 100%")
            // For updates, we don't strictly enforce future insurance expiry to allow editing existing records
            else -> vehicleRepository.updateVehicle(vehicle)
        }
    }
}

/**
 * Use case to delete a vehicle.
 */
class DeleteVehicleUseCase @Inject constructor(
    private val vehicleRepository: VehicleRepository
) {
    suspend operator fun invoke(vehicleId: String): Result<Boolean> {
        return when {
            vehicleId.isBlank() -> Result.Error("Vehicle ID cannot be empty")
            else -> vehicleRepository.deleteVehicle(vehicleId)
        }
    }
}
