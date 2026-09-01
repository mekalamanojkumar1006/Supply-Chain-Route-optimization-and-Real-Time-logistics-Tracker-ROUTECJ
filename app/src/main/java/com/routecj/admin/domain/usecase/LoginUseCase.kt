package com.routecj.admin.domain.usecase

import com.routecj.admin.core.util.Result
import com.routecj.admin.domain.model.Admin
import com.routecj.admin.domain.repository.AuthRepository
import javax.inject.Inject

/**
 * Use case for performing user login.
 * Encapsulates the business logic for authentication.
 */
class LoginUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    /**
     * Executes the login operation.
     * 
     * @param email The user's email address.
     * @param password The user's password.
     * @return A Result containing the authenticated Admin or an error.
     */
    suspend operator fun invoke(email: String, password: String): Result<Admin> {
        // Business-level validation
        if (email.isBlank()) {
            return Result.Error("Email is required")
        }
        if (password.isBlank()) {
            return Result.Error("Password is required")
        }

        // Delegate authentication to the repository
        return authRepository.login(email, password)
    }
}
