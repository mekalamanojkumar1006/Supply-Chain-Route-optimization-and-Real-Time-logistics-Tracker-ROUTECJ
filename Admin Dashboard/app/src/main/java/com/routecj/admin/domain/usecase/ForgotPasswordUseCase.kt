package com.routecj.admin.domain.usecase

import com.routecj.admin.core.util.Result
import com.routecj.admin.domain.repository.AuthRepository
import javax.inject.Inject

/**
 * Use case to handle password reset functionality.
 */
class ForgotPasswordUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(email: String): Result<Unit> {
        if (email.isBlank()) {
            return Result.Error("Email cannot be empty")
        }
        return authRepository.sendPasswordResetEmail(email)
    }
}
