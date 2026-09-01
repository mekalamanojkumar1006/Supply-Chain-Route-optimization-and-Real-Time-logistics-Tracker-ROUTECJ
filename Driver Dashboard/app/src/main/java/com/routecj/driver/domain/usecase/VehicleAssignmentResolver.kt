package com.routecj.driver.domain.usecase

import com.routecj.driver.core.util.Result
import com.routecj.driver.domain.model.Vehicle
import com.routecj.driver.domain.repository.VehicleRepository

enum class VehicleReferenceSource {
    TRIP_DISPATCH,
    DRIVER_PROFILE,
    BOTH_MATCHED
}

data class ResolvedVehicleAssignment(
    val vehicle: Vehicle,
    val canonicalVehicleId: String,
    val registrationNumber: String,
    val vehicleNumber: String,
    val source: VehicleReferenceSource
)

sealed interface VehicleResolutionResult {
    data class Success(val assignment: ResolvedVehicleAssignment) : VehicleResolutionResult
    object NoVehicleReference : VehicleResolutionResult
    data class RecordNotFound(val reference: String) : VehicleResolutionResult
    data class VehicleMismatch(val driverVehicleId: String, val tripVehicleId: String) : VehicleResolutionResult
}

/**
 * Domain-level Single Source of Truth for resolving vehicle assignments.
 * Canonicalizes vehicle references from Trip/Dispatch records and Driver Profile records
 * against the Firestore 'vehicles' collection.
 */
class VehicleAssignmentResolver(
    private val vehicleRepository: VehicleRepository
) {

    suspend fun resolve(
        tripVehicleRef: String?,
        driverVehicleRef: String?
    ): VehicleResolutionResult {
        val cleanTripRef = tripVehicleRef?.trim()?.takeIf { it.isNotBlank() }
        val cleanDriverRef = driverVehicleRef?.trim()?.takeIf { it.isNotBlank() }

        if (cleanTripRef == null && cleanDriverRef == null) {
            return VehicleResolutionResult.NoVehicleReference
        }

        var resolvedTripVehicle: Vehicle? = null
        if (cleanTripRef != null) {
            when (val res = vehicleRepository.getVehicleById(cleanTripRef)) {
                is Result.Success -> resolvedTripVehicle = res.data
                is Result.Error -> return VehicleResolutionResult.RecordNotFound(cleanTripRef)
                is Result.Loading -> return VehicleResolutionResult.RecordNotFound(cleanTripRef)
            }
        }

        var resolvedDriverVehicle: Vehicle? = null
        if (cleanDriverRef != null) {
            when (val res = vehicleRepository.getVehicleById(cleanDriverRef)) {
                is Result.Success -> resolvedDriverVehicle = res.data
                is Result.Error -> return VehicleResolutionResult.RecordNotFound(cleanDriverRef)
                is Result.Loading -> return VehicleResolutionResult.RecordNotFound(cleanDriverRef)
            }
        }

        // Both references exist: verify they resolve to the same canonical vehicle
        if (resolvedTripVehicle != null && resolvedDriverVehicle != null) {
            if (resolvedTripVehicle.id != resolvedDriverVehicle.id) {
                return VehicleResolutionResult.VehicleMismatch(
                    driverVehicleId = resolvedDriverVehicle.id,
                    tripVehicleId = resolvedTripVehicle.id
                )
            }
            return VehicleResolutionResult.Success(
                ResolvedVehicleAssignment(
                    vehicle = resolvedTripVehicle,
                    canonicalVehicleId = resolvedTripVehicle.id,
                    registrationNumber = resolvedTripVehicle.registrationNumber.ifBlank { resolvedTripVehicle.vehicleNumber },
                    vehicleNumber = resolvedTripVehicle.vehicleNumber.ifBlank { resolvedTripVehicle.registrationNumber },
                    source = VehicleReferenceSource.BOTH_MATCHED
                )
            )
        }

        // Authoritative resolution from whichever reference is present
        val effectiveVehicle = resolvedTripVehicle ?: resolvedDriverVehicle!!
        val source = if (resolvedTripVehicle != null) VehicleReferenceSource.TRIP_DISPATCH else VehicleReferenceSource.DRIVER_PROFILE

        return VehicleResolutionResult.Success(
            ResolvedVehicleAssignment(
                vehicle = effectiveVehicle,
                canonicalVehicleId = effectiveVehicle.id,
                registrationNumber = effectiveVehicle.registrationNumber.ifBlank { effectiveVehicle.vehicleNumber },
                vehicleNumber = effectiveVehicle.vehicleNumber.ifBlank { effectiveVehicle.registrationNumber },
                source = source
            )
        )
    }
}
