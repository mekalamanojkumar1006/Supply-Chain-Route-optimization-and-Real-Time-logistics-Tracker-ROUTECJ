package com.routecj.admin.data.repository

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.routecj.admin.core.util.Result
import com.routecj.admin.data.model.AdminDto
import com.routecj.admin.domain.model.Admin
import com.routecj.admin.domain.model.AdminRole
import com.routecj.admin.domain.model.Driver
import com.routecj.admin.domain.model.DriverStatus
import com.routecj.admin.domain.repository.AccountProvisioningRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AccountProvisioningRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val firestore: FirebaseFirestore
) : AccountProvisioningRepository {

    companion object {
        private const val PROVISIONING_APP_NAME = "RouteCJProvisioningApp"
    }

    /**
     * Obtains an isolated secondary FirebaseAuth instance so account provisioning
     * does NOT overwrite or log out the currently authenticated admin's session.
     */
    private fun getProvisioningAuth(): FirebaseAuth {
        val secondaryApp = try {
            FirebaseApp.getInstance(PROVISIONING_APP_NAME)
        } catch (_: IllegalStateException) {
            val defaultApp = FirebaseApp.getInstance()
            FirebaseApp.initializeApp(context, defaultApp.options, PROVISIONING_APP_NAME)
        }
        return FirebaseAuth.getInstance(secondaryApp)
    }

    override suspend fun checkEmailExists(email: String): Result<Boolean> = try {
        val cleanEmail = email.trim().lowercase()

        // 1. Check in drivers collection
        val driverDocs = firestore.collection("drivers")
            .whereEqualTo("email", cleanEmail)
            .limit(1)
            .get()
            .await()

        if (!driverDocs.isEmpty) {
            Result.Success(true)
        } else {
            // 2. Check in admins collection
            val adminDocs = firestore.collection("admins")
                .whereEqualTo("email", cleanEmail)
                .limit(1)
                .get()
                .await()

            Result.Success(!adminDocs.isEmpty)
        }
    } catch (e: Exception) {
        Timber.tag("PROVISIONING").e(e, "Error checking email existence: $email")
        Result.Error(e.message ?: "Failed to verify email availability", throwable = e)
    }

    override suspend fun checkPhoneExists(phone: String): Result<Boolean> = try {
        val cleanPhone = phone.trim()

        val driverDocs = firestore.collection("drivers")
            .whereEqualTo("phone", cleanPhone)
            .limit(1)
            .get()
            .await()

        if (!driverDocs.isEmpty) {
            Result.Success(true)
        } else {
            val adminDocs = firestore.collection("admins")
                .whereEqualTo("phone", cleanPhone)
                .limit(1)
                .get()
                .await()

            Result.Success(!adminDocs.isEmpty)
        }
    } catch (e: Exception) {
        Timber.tag("PROVISIONING").e(e, "Error checking phone existence: $phone")
        Result.Error(e.message ?: "Failed to verify phone availability", throwable = e)
    }

    override suspend fun createDriverAccount(driver: Driver, temporaryPassword: String): Result<Driver> {
        return try {
            val cleanEmail = driver.email.trim().lowercase()
            val cleanPhone = driver.phone.trim()

            if (cleanEmail.isBlank()) {
                return Result.Error("Driver company email cannot be empty")
            }
            if (temporaryPassword.length < 6) {
                return Result.Error("Temporary password must be at least 6 characters")
            }

            // Duplicate checks
            val emailCheck = checkEmailExists(cleanEmail)
            if (emailCheck is Result.Success && emailCheck.data) {
                return Result.Error("Driver account with email '$cleanEmail' already exists")
            }
            if (cleanPhone.isNotBlank()) {
                val phoneCheck = checkPhoneExists(cleanPhone)
                if (phoneCheck is Result.Success && phoneCheck.data) {
                    return Result.Error("Account with phone '$cleanPhone' already exists")
                }
            }

            // 1. Create Firebase Authentication Account via secondary instance
            val provisioningAuth = getProvisioningAuth()
            val authResult = provisioningAuth.createUserWithEmailAndPassword(cleanEmail, temporaryPassword).await()
            val firebaseUser = authResult.user ?: throw IllegalStateException("Firebase Auth failed to return UID")
            val authUid = firebaseUser.uid
            provisioningAuth.signOut() // Clear secondary session immediately

            Timber.tag("PROVISIONING").d("Created Firebase Auth credentials for driver: UID=$authUid, Email=$cleanEmail")

            // 2. Prepare driver document in Firestore
            val docRef = if (driver.id.isNotBlank()) {
                firestore.collection("drivers").document(driver.id)
            } else {
                firestore.collection("drivers").document()
            }

            val driverId = docRef.id
            val now = Date()

            val fullDriver = driver.copy(
                id = driverId,
                uid = authUid,
                email = cleanEmail,
                role = "DRIVER",
                status = DriverStatus.AVAILABLE,
                isActive = true,
                createdAt = now,
                joinedDate = now,
                lastActive = now
            )

            val driverMap = mapOf(
                "driverId" to driverId,
                "id" to driverId,
                "uid" to authUid,
                "name" to fullDriver.name,
                "email" to cleanEmail,
                "phone" to cleanPhone,
                "role" to "DRIVER",
                "licenseNumber" to fullDriver.licenseNumber,
                "licenseExpiryDate" to fullDriver.licenseExpiryDate,
                "status" to "AVAILABLE",
                "assignedVehicle" to (fullDriver.assignedVehicle ?: ""),
                "assignedVehicleId" to (fullDriver.assignedVehicleId ?: ""),
                "rating" to fullDriver.rating,
                "totalDeliveries" to fullDriver.totalDeliveries,
                "completedDeliveries" to fullDriver.completedDeliveries,
                "profileImage" to (fullDriver.profileImage ?: ""),
                "currentLatitude" to fullDriver.currentLatitude,
                "currentLongitude" to fullDriver.currentLongitude,
                "address" to fullDriver.address,
                "isActive" to true,
                "joinedDate" to fullDriver.joinedDate,
                "lastActive" to fullDriver.lastActive,
                "createdAt" to fullDriver.createdAt
            )

            docRef.set(driverMap).await()
            Timber.tag("PROVISIONING").d("Saved driver profile to /drivers/$driverId")

            Result.Success(fullDriver)
        } catch (e: com.google.firebase.auth.FirebaseAuthUserCollisionException) {
            Timber.tag("PROVISIONING").e(e, "Firebase Auth User Collision")
            Result.Error("Driver account already exists in Firebase Authentication")
        } catch (e: Exception) {
            Timber.tag("PROVISIONING").e(e, "Failed to create driver account")
            Result.Error(e.message ?: "Failed to create driver account", throwable = e)
        }
    }

    override suspend fun createAdminAccount(admin: Admin, temporaryPassword: String): Result<Admin> {
        return try {
            val cleanEmail = admin.email.trim().lowercase()
            val cleanPhone = admin.phone.trim()

            if (cleanEmail.isBlank()) {
                return Result.Error("Admin email cannot be empty")
            }
            if (temporaryPassword.length < 6) {
                return Result.Error("Temporary password must be at least 6 characters")
            }
            if (admin.role == AdminRole.UNKNOWN) {
                return Result.Error("Please assign a valid administrative role")
            }

            // Duplicate checks
            val emailCheck = checkEmailExists(cleanEmail)
            if (emailCheck is Result.Success && emailCheck.data) {
                return Result.Error("Admin account with email '$cleanEmail' already exists")
            }
            if (cleanPhone.isNotBlank()) {
                val phoneCheck = checkPhoneExists(cleanPhone)
                if (phoneCheck is Result.Success && phoneCheck.data) {
                    return Result.Error("Account with phone '$cleanPhone' already exists")
                }
            }

            // 1. Create Firebase Authentication Account via secondary instance
            val provisioningAuth = getProvisioningAuth()
            val authResult = provisioningAuth.createUserWithEmailAndPassword(cleanEmail, temporaryPassword).await()
            val firebaseUser = authResult.user ?: throw IllegalStateException("Firebase Auth failed to return UID")
            val authUid = firebaseUser.uid
            provisioningAuth.signOut() // Clear secondary session immediately

            Timber.tag("PROVISIONING").d("Created Firebase Auth credentials for admin: UID=$authUid, Email=$cleanEmail")

            // 2. Prepare admin document in Firestore under /admins/{uid}
            val adminId = if (admin.adminId.isNotBlank()) admin.adminId else admin.role.roleId

            val fullAdmin = admin.copy(
                uid = authUid,
                adminId = adminId,
                email = cleanEmail,
                status = "ACTIVE"
            )

            val adminMap = mapOf(
                "uid" to authUid,
                "adminId" to adminId,
                "name" to fullAdmin.name,
                "email" to cleanEmail,
                "phone" to cleanPhone,
                "role" to fullAdmin.role.name, // e.g. "ADMIN", "GODOWN_MANAGER", "DISPATCH_MANAGER"
                "status" to "ACTIVE",
                "lastLogin" to "",
                "profileImage" to (fullAdmin.profileImage ?: ""),
                "notificationsEnabled" to fullAdmin.notificationsEnabled,
                "orderAlertsEnabled" to fullAdmin.orderAlertsEnabled,
                "dispatchAlertsEnabled" to fullAdmin.dispatchAlertsEnabled,
                "driverAlertsEnabled" to fullAdmin.driverAlertsEnabled,
                "createdAt" to Date()
            )

            firestore.collection("admins").document(authUid).set(adminMap).await()
            Timber.tag("PROVISIONING").d("Saved admin profile to /admins/$authUid")

            Result.Success(fullAdmin)
        } catch (e: com.google.firebase.auth.FirebaseAuthUserCollisionException) {
            Timber.tag("PROVISIONING").e(e, "Firebase Auth User Collision")
            Result.Error("Admin account already exists in Firebase Authentication")
        } catch (e: Exception) {
            Timber.tag("PROVISIONING").e(e, "Failed to create admin account")
            Result.Error(e.message ?: "Failed to create admin account", throwable = e)
        }
    }

    override suspend fun updateAdminStatus(adminId: String, status: String): Result<Unit> = try {
        val updates = mapOf(
            "status" to status.uppercase(),
            "updatedAt" to Date()
        )
        firestore.collection("admins").document(adminId).update(updates).await()
        Result.Success(Unit)
    } catch (e: Exception) {
        Timber.tag("PROVISIONING").e(e, "Failed to update admin status: $adminId")
        Result.Error(e.message ?: "Failed to update admin status", throwable = e)
    }

    override suspend fun updateDriverStatus(
        driverId: String,
        status: DriverStatus,
        isActive: Boolean
    ): Result<Unit> = try {
        val updates = mapOf(
            "status" to status.name.uppercase(),
            "isActive" to isActive,
            "lastActive" to Date()
        )
        firestore.collection("drivers").document(driverId).update(updates).await()
        Result.Success(Unit)
    } catch (e: Exception) {
        Timber.tag("PROVISIONING").e(e, "Failed to update driver status: $driverId")
        Result.Error(e.message ?: "Failed to update driver status", throwable = e)
    }

    override fun getAllAdmins(): Flow<Result<List<Admin>>> = callbackFlow {
        val listener = firestore.collection("admins")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Result.Error(error.message ?: "Failed to fetch admins", throwable = error))
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val admins = snapshot.documents.mapNotNull { doc ->
                        try {
                            val dto = doc.toObject(AdminDto::class.java)
                            dto?.toDomain()?.copy(
                                uid = if (dto.uid.isNotBlank()) dto.uid else doc.id
                            )
                        } catch (e: Exception) {
                            Timber.tag("PROVISIONING").e(e, "Error parsing admin ${doc.id}")
                            null
                        }
                    }
                    trySend(Result.Success(admins))
                }
            }
        awaitClose { listener.remove() }
    }
}
