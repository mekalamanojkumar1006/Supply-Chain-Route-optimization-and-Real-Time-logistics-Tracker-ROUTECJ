package com.routecj.driver.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.routecj.driver.core.util.Result
import com.routecj.driver.data.mapper.DriverMapper
import com.routecj.driver.domain.model.Driver
import com.routecj.driver.domain.repository.DriverRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.Date

/**
 * Firestore implementation of DriverRepository.
 * Interacts with 'drivers' collection in RouteCJ Firebase.
 */
class FirestoreDriverRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : DriverRepository {

    private suspend fun requireAuth(driverId: String) {
        val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
            ?: throw Exception("DRIVER NOT AUTHORIZED: You must be logged in.")

        val authUid = currentUser.uid
        if (authUid == driverId) {
            return
        }

        if (driverId.isNotBlank()) {
            val driverDoc = firestore.collection("drivers").document(driverId).get().await()
            if (driverDoc.exists()) {
                val docUid = driverDoc.getString("uid") ?: driverDoc.getString("authUid") ?: driverDoc.getString("userId")
                val docEmail = driverDoc.getString("email")

                val matchesUid = !docUid.isNullOrBlank() && docUid == authUid
                val matchesEmail = !docEmail.isNullOrBlank() && !currentUser.email.isNullOrBlank() &&
                        docEmail.equals(currentUser.email, ignoreCase = true)

                if (matchesUid || matchesEmail) {
                    return
                }
            }

            val authDriverDoc = firestore.collection("drivers").document(authUid).get().await()
            if (authDriverDoc.exists()) {
                val mappedId = authDriverDoc.getString("driverId") ?: authDriverDoc.getString("id")
                if (mappedId == driverId) {
                    return
                }
            }
        }

        throw Exception("TRIP ACCESS DENIED: Unauthorized driver.")
    }

    override suspend fun getDriverById(driverId: String): Result<Driver> = try {
        val doc = firestore.collection("drivers").document(driverId).get().await()
        if (doc.exists()) {
            Result.Success(DriverMapper.mapToDomain(doc.id, doc.data))
        } else {
            Result.Error("Driver profile not found")
        }
    } catch (e: Exception) {
        Result.Error(e.message ?: "Failed to fetch driver profile", e)
    }

    override fun observeDriverById(driverId: String): Flow<Result<Driver>> = callbackFlow {
        val listener = firestore.collection("drivers").document(driverId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Result.Error(error.message ?: "Error observing driver profile", error))
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    try {
                        val driver = DriverMapper.mapToDomain(snapshot.id, snapshot.data)
                        trySend(Result.Success(driver))
                    } catch (e: Exception) {
                        trySend(Result.Error("Failed to parse driver document: ${e.message}", e))
                    }
                } else {
                    trySend(Result.Error("Driver profile document does not exist"))
                }
            }
        awaitClose { listener.remove() }
    }

    override suspend fun updateDriverLocation(
        driverId: String,
        latitude: Double,
        longitude: Double
    ): Result<Unit> = try {
        requireAuth(driverId)
        firestore.collection("drivers").document(driverId)
            .update(
                mapOf(
                    "currentLatitude" to latitude,
                    "currentLongitude" to longitude,
                    "lastActive" to Date()
                )
            ).await()
        Result.Success(Unit)
    } catch (e: Exception) {
        Result.Error(e.message ?: "Failed to update driver location", e)
    }

    override suspend fun updateDriverStatus(driverId: String, status: String): Result<Unit> = try {
        requireAuth(driverId)
        firestore.collection("drivers").document(driverId)
            .update(
                mapOf(
                    "status" to status.lowercase(),
                    "lastActive" to Date()
                )
            ).await()
        Result.Success(Unit)
    } catch (e: Exception) {
        Result.Error(e.message ?: "Failed to update driver status", e)
    }
}
