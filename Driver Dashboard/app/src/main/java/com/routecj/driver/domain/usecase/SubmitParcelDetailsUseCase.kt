package com.routecj.driver.domain.usecase

import com.routecj.driver.core.util.Result
import com.routecj.driver.domain.model.ParcelSubmissionData
import com.routecj.driver.domain.repository.OrderRepository

/**
 * UseCase to validate and submit parcel details to the Godown Manager.
 */
class SubmitParcelDetailsUseCase(
    private val orderRepository: OrderRepository
) {

    suspend operator fun invoke(
        orderId: String,
        driverId: String,
        itemDescription: String,
        packageCountStr: String,
        weightStr: String,
        specialInstructions: String
    ): Result<Unit> {
        if (orderId.isBlank()) {
            return Result.Error("Invalid order identifier.")
        }
        if (driverId.isBlank()) {
            return Result.Error("TRIP ACCESS DENIED: Invalid driver identity.")
        }

        val trimmedDesc = itemDescription.trim()
        if (trimmedDesc.isBlank()) {
            return Result.Error("Enter item description.")
        }

        val packageCount = packageCountStr.trim().toIntOrNull()
        if (packageCount == null || packageCount <= 0) {
            return Result.Error("Package count must be at least 1.")
        }

        val weight = if (weightStr.isBlank()) 0.0 else weightStr.trim().toDoubleOrNull()
        if (weight == null || weight < 0.0) {
            return Result.Error("Enter a valid weight.")
        }

        val parcelData = ParcelSubmissionData(
            itemDescription = trimmedDesc,
            packageCount = packageCount,
            weight = weight,
            specialInstructions = specialInstructions.trim()
        )

        return orderRepository.submitParcelDetails(orderId, driverId, parcelData)
    }
}
