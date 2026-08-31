package com.routecj.admin.domain.repository

import com.routecj.admin.core.util.Result
import com.routecj.admin.domain.model.Admin

/**
 * Repository interface for profile-related operations.
 * Defines the contract for fetching admin profile data.
 */
interface ProfileRepository {

    /**
     * Fetches the current logged-in admin's profile using their UID from Firebase.
     * 
     * @return Result.Success(Admin) if found, Result.Error if not found or error occurs
     */
    suspend fun getCurrentAdminProfile(): Result<Admin>

    /**
     * Updates the admin profile in Firestore.
     */
    suspend fun updateAdminProfile(admin: Admin): Result<Unit>
}
