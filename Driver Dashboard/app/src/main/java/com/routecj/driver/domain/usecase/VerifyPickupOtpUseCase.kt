package com.routecj.driver.domain.usecase

import com.routecj.driver.core.util.Result
import com.routecj.driver.domain.repository.OrderRepository

/**
 * UseCase to verify customer pickup OTP.
 * Validates input length (4-6 digits) and triggers atomic server verification via OrderRepository.
 */
class VerifyPickupOtpUseCase(
    private val orderRepository: OrderRepository
) {

    suspend operator fun invoke(orderId: String, enteredOtp: String, driverId: String): Result<Unit> {
        if (orderId.isBlank() || driverId.isBlank()) {
            return Result.Error("Invalid order or driver identification")
        }

        val cleanOtp = enteredOtp.trim()
        if (cleanOtp.length < 4 || cleanOtp.length > 6 || !cleanOtp.all { it.isDigit() }) {
            return Result.Error("Please enter a valid numeric pickup code")
        }

        return orderRepository.verifyPickupOtp(orderId, cleanOtp, driverId)
    }
}
