package com.routecj.driver.domain.usecase

import com.routecj.driver.core.util.Result
import com.routecj.driver.domain.model.Driver
import com.routecj.driver.domain.model.Vehicle
import com.routecj.driver.domain.repository.DriverRepository
import com.routecj.driver.domain.repository.VehicleRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf

/**
 * Composite data representing the full Driver Profile + Vehicle state.
 */
data class DriverProfileData(
    val driver: Driver,
    val vehicle: Vehicle?
)

/**
 * UseCase to observe live Driver Profile and Assigned Vehicle data.
 */
class GetDriverProfileUseCase(
    private val driverRepository: DriverRepository,
    private val vehicleRepository: VehicleRepository
) {
    operator fun invoke(driverId: String): Flow<Result<DriverProfileData>> {
        return driverRepository.observeDriverById(driverId).combine(flowOf(Unit)) { driverRes, _ ->
            when (driverRes) {
                is Result.Success -> {
                    val driver = driverRes.data
                    val vehicleId = driver.assignedVehicleId ?: driver.assignedVehicle

                    val vehicle = if (!vehicleId.isNullOrBlank()) {
                        when (val vRes = vehicleRepository.getVehicleById(vehicleId)) {
                            is Result.Success -> vRes.data
                            else -> null
                        }
                    } else null

                    Result.Success(DriverProfileData(driver = driver, vehicle = vehicle))
                }
                is Result.Error -> Result.Error(driverRes.message, driverRes.throwable)
                is Result.Loading -> Result.Loading
            }
        }
    }
}
