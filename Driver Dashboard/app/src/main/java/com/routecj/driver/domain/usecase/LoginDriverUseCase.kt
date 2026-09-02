package com.routecj.driver.domain.usecase

import com.routecj.driver.core.util.Result
import com.routecj.driver.domain.model.Driver
import com.routecj.driver.domain.repository.AuthRepository

/**
 * UseCase to authenticate a Driver via email and password,
 * verifying their presence in the 'drivers' collection.
 */
class LoginDriverUseCase(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(email: String, password: String): Result<Driver> {
        if (email.isBlank()) {
            return Result.Error("Enter your email.")
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()) {
            return Result.Error("Enter a valid email address.")
        }
        if (password.isBlank()) {
            return Result.Error("Enter your password.")
        }
        return authRepository.login(email, password)
    }
}
