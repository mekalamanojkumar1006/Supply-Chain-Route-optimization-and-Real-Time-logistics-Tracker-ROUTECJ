package com.routecj.admin.domain.repository

import com.routecj.admin.core.util.Result
import com.routecj.admin.domain.model.Admin
import com.routecj.admin.domain.model.Driver
import com.routecj.admin.domain.model.DriverStatus
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for official RouteCJ account creation & identity management.
 * 
 * Secure provisioning rules:
 * - Credentials handled exclusively by Firebase Authentication Email/Password
 * - Passwords never saved in Firestore or local storage
 * - No disruption to current logged-in admin session (uses isolated secondary Firebase instance)
 * - Strict duplicate account checking before creation
 */
interface AccountProvisioningRepository {

    /**
     * Checks whether an email address is already in use in Firebase Auth or Firestore profiles.
     */
    suspend fun checkEmailExists(email: String): Result<Boolean>

    /**
     * Checks whether a phone number is already in use by any driver or admin profile.
     */
    suspend fun checkPhoneExists(phone: String): Result<Boolean>

    /**
     * Creates a new official Driver account.
     * 1. Creates Firebase Auth user using email & temporary password.
     * 2. Creates `/drivers/{driverId}` document with UID, role = DRIVER, status = AVAILABLE, isActive = true.
     */
    suspend fun createDriverAccount(driver: Driver, temporaryPassword: String): Result<Driver>

    /**
     * Creates a new official Admin account (ADMIN, GODOWN_MANAGER, DISPATCH_MANAGER).
     * 1. Creates Firebase Auth user using email & temporary password.
     * 2. Creates `/admins/{uid}` document with assigned role and active status.
     */
    suspend fun createAdminAccount(admin: Admin, temporaryPassword: String): Result<Admin>

    /**
     * Updates an admin account status (ACTIVE, INACTIVE, SUSPENDED).
     */
    suspend fun updateAdminStatus(adminId: String, status: String): Result<Unit>

    /**
     * Updates a driver account status and active state.
     */
    suspend fun updateDriverStatus(driverId: String, status: DriverStatus, isActive: Boolean): Result<Unit>

    /**
     * Streams real-time list of all admin accounts from `/admins`.
     */
    fun getAllAdmins(): Flow<Result<List<Admin>>>
}
