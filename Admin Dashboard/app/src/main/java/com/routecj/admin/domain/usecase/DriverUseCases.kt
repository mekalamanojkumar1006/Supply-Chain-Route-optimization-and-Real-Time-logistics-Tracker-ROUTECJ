package com.routecj.admin.domain.usecase

import com.routecj.admin.core.util.Result
import com.routecj.admin.domain.model.Driver
import com.routecj.admin.domain.repository.DriverRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case to retrieve and listen to live updates of all drivers.
 */
class GetDriversUseCase @Inject constructor(
    private val driverRepository: DriverRepository
) {
    suspend operator fun invoke(): Flow<Result<List<Driver>>> {
        return driverRepository.getAllDrivers()
    }
}

/**
 * Use case to create a new driver profile.
 */
class CreateDriverUseCase @Inject constructor(
    private val driverRepository: DriverRepository
) {
    suspend operator fun invoke(driver: Driver): Result<Driver> {
        return when {
            driver.name.isBlank() -> Result.Error("Driver name cannot be empty")
            driver.phone.isBlank() -> Result.Error("Phone number cannot be empty")
            driver.licenseNumber.isBlank() -> Result.Error("License number cannot be empty")
            else -> driverRepository.createDriver(driver)
        }
    }
}

/**
 * Use case to update an existing driver profile.
 */
class UpdateDriverUseCase @Inject constructor(
    private val driverRepository: DriverRepository
) {
    suspend operator fun invoke(driver: Driver): Result<Driver> {
        return when {
            driver.id.isBlank() -> Result.Error("Driver ID cannot be empty")
            driver.name.isBlank() -> Result.Error("Driver name cannot be empty")
            driver.phone.isBlank() -> Result.Error("Phone number cannot be empty")
            else -> driverRepository.updateDriver(driver)
        }
    }
}

/**
 * Use case to delete a driver profile.
 */
class DeleteDriverUseCase @Inject constructor(
    private val driverRepository: DriverRepository
) {
    suspend operator fun invoke(driverId: String): Result<Boolean> {
        return when {
            driverId.isBlank() -> Result.Error("Driver ID cannot be empty")
            else -> driverRepository.deleteDriver(driverId)
        }
    }
}
