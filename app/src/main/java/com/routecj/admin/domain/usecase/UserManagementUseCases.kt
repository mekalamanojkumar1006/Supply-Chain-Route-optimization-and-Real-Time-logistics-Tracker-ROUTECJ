package com.routecj.admin.domain.usecase

import com.routecj.admin.core.util.Result
import com.routecj.admin.domain.model.Admin
import com.routecj.admin.domain.model.AdminRole
import com.routecj.admin.domain.model.Driver
import com.routecj.admin.domain.model.DriverStatus
import com.routecj.admin.domain.repository.AccountProvisioningRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case to create a new RouteCJ Driver account.
 */
class CreateDriverAccountUseCase @Inject constructor(
    private val repository: AccountProvisioningRepository
) {
    suspend operator fun invoke(driver: Driver, temporaryPassword: String): Result<Driver> {
        return when {
            driver.name.isBlank() -> Result.Error("Driver name cannot be empty")
            driver.phone.isBlank() -> Result.Error("Phone number cannot be empty")
            driver.email.isBlank() -> Result.Error("Driver company email cannot be empty")
            temporaryPassword.length < 6 -> Result.Error("Temporary password must be at least 6 characters")
            else -> repository.createDriverAccount(driver, temporaryPassword)
        }
    }
}

/**
 * Use case to create a new RouteCJ Admin account (ADMIN, GODOWN_MANAGER, DISPATCH_MANAGER).
 */
class CreateAdminAccountUseCase @Inject constructor(
    private val repository: AccountProvisioningRepository
) {
    suspend operator fun invoke(admin: Admin, temporaryPassword: String): Result<Admin> {
        return when {
            admin.name.isBlank() -> Result.Error("Admin name cannot be empty")
            admin.email.isBlank() -> Result.Error("Admin company email cannot be empty")
            admin.role == AdminRole.UNKNOWN -> Result.Error("Please select a valid administrative role")
            temporaryPassword.length < 6 -> Result.Error("Temporary password must be at least 6 characters")
            else -> repository.createAdminAccount(admin, temporaryPassword)
        }
    }
}

/**
 * Use case to stream all administrators.
 */
class GetAdminUsersUseCase @Inject constructor(
    private val repository: AccountProvisioningRepository
) {
    operator fun invoke(): Flow<Result<List<Admin>>> {
        return repository.getAllAdmins()
    }
}

/**
 * Use case to update account status (ACTIVE, INACTIVE, SUSPENDED).
 */
class UpdateUserStatusUseCase @Inject constructor(
    private val repository: AccountProvisioningRepository
) {
    suspend fun updateAdminStatus(adminId: String, status: String): Result<Unit> {
        return if (adminId.isBlank()) {
            Result.Error("Admin ID cannot be empty")
        } else {
            repository.updateAdminStatus(adminId, status)
        }
    }

    suspend fun updateDriverStatus(driverId: String, status: DriverStatus, isActive: Boolean): Result<Unit> {
        return if (driverId.isBlank()) {
            Result.Error("Driver ID cannot be empty")
        } else {
            repository.updateDriverStatus(driverId, status, isActive)
        }
    }
}

/**
 * Use case to check for duplicate accounts.
 */
class CheckDuplicateAccountUseCase @Inject constructor(
    private val repository: AccountProvisioningRepository
) {
    suspend fun checkEmail(email: String): Result<Boolean> = repository.checkEmailExists(email)
    suspend fun checkPhone(phone: String): Result<Boolean> = repository.checkPhoneExists(phone)
}
