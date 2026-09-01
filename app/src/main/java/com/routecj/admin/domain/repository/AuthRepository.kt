package com.routecj.admin.domain.repository

import com.routecj.admin.core.util.Result
import com.routecj.admin.domain.model.Admin
import kotlinx.coroutines.flow.Flow

/**
 * Interface defining the authentication contract for the application.
 * Following Clean Architecture, this remains independent of any specific
 * implementation (like Firebase).
 */
interface AuthRepository {

    /**
     * Authenticates a user using email and password.
     * Returns Result<Admin> containing the authenticated user's details.
     */
    suspend fun login(email: String, password: String): Result<Admin>

    /**
     * Retrieves the current logged-in Admin's details from the data source.
     * Returns Result.Success(Admin) if found, Result.Success(null) if no session,
     * or Result.Error if a data fetch error occurs.
     */
    suspend fun getCurrentAdmin(): Result<Admin?>

    /**
     * Observes the current admin's profile in real-time.
     */
    fun observeAdminProfile(uid: String): Flow<Result<Admin>>

    /**
     * Signs out the current user and clears the session.
     */
    suspend fun logout()

    /**
     * Checks if a user session currently exists.
     * Non-suspending because checking a local session token is typically fast.
     */
    fun isUserLoggedIn(): Boolean

    /**
     * Send password reset email.
     */
    suspend fun sendPasswordResetEmail(email: String): Result<Unit>

    /**
     * Changes the current user's password.
     */
    suspend fun changePassword(currentPassword: String, newPassword: String): Result<Unit>
}
