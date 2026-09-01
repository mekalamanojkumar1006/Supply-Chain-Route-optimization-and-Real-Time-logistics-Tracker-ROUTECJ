package com.routecj.admin.domain.usecase

import com.routecj.admin.core.util.Result
import com.routecj.admin.domain.model.Admin
import com.routecj.admin.domain.repository.ProfileRepository
import javax.inject.Inject

/**
 * Use case for updating the admin's profile.
 */
class UpdateAdminProfileUseCase @Inject constructor(
    private val profileRepository: ProfileRepository
) {
    suspend operator fun invoke(admin: Admin): Result<Unit> {
        return when {
            admin.name.isBlank() -> Result.Error("Name cannot be empty")
            admin.phone.isBlank() -> Result.Error("Phone number cannot be empty")
            else -> profileRepository.updateAdminProfile(admin)
        }
    }
}
