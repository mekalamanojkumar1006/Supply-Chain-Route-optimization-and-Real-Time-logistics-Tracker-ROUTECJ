package com.routecj.driver.domain.usecase

import com.routecj.driver.domain.repository.AuthRepository

/**
 * UseCase to log out the authenticated Driver.
 */
class LogoutDriverUseCase(
    private val authRepository: AuthRepository
) {
    operator fun invoke() {
        authRepository.logout()
    }
}
