package com.routecj.driver.domain.usecase

import com.routecj.driver.core.util.Result
import com.routecj.driver.domain.model.Driver
import com.routecj.driver.domain.repository.AuthRepository

/**
 * UseCase to get current authenticated and authorized Driver.
 */
class GetCurrentDriverUseCase(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(): Result<Driver> {
        return authRepository.getCurrentDriver()
    }
}
