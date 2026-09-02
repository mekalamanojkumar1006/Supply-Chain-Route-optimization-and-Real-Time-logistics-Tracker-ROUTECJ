package com.routecj.driver.domain.usecase

import com.routecj.driver.core.util.Result
import com.routecj.driver.domain.repository.AuthRepository

/**
 * UseCase to send password reset email for Driver account.
 */
class SendPasswordResetUseCase(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(email: String): Result<Unit> {
        if (email.isBlank()) {
            return Result.Error("Enter your email.")
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()) {
            return Result.Error("Enter a valid email address.")
        }
        return authRepository.sendPasswordReset(email)
    }
}
