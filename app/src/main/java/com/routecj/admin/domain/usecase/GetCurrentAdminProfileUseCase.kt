package com.routecj.admin.domain.usecase

import com.routecj.admin.core.util.Result
import com.routecj.admin.domain.model.Admin
import com.routecj.admin.domain.repository.ProfileRepository
import javax.inject.Inject

/**
 * Use case for fetching the current logged-in admin's profile.
 * Encapsulates the business logic for profile retrieval.
 */
class GetCurrentAdminProfileUseCase @Inject constructor(
    private val profileRepository: ProfileRepository
) {
    /**
     * Executes the profile fetch operation.
     * 
     * @return A Result containing the Admin's profile or an error.
     */
    suspend operator fun invoke(): Result<Admin> {
        return profileRepository.getCurrentAdminProfile()
    }
}
