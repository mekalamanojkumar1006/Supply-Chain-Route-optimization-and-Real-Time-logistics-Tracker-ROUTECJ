package com.routecj.admin.data.repository

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.routecj.admin.core.util.Result
import com.routecj.admin.domain.model.Driver
import com.routecj.admin.domain.model.DriverStatus
import com.routecj.admin.domain.repository.DriverRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.Date
import javax.inject.Inject

/**
 * Firestore implementation of DriverRepository.
 * Handles realtime tracking and sync with the Firestore 'drivers' collection.
 */
class FirestoreDriverRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) : DriverRepository {

    override suspend fun getAllDrivers(): Flow<Result<List<Driver>>> = callbackFlow {
        val listener = firestore.collection("drivers")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Result.Error(error.message ?: "Failed to listen to drivers", throwable = error))
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val driversList = snapshot.documents.mapNotNull { doc ->
                        try {
                            docToDriver(doc.id, doc.data)
                        } catch (e: Exception) {
                            Log.e("FIRESTORE_DRIVER_REPO", "Error parsing driver ${doc.id}", e)
                            null
                        }
                    }
                    trySend(Result.Success(driversList))
                }
            }
        awaitClose { listener.remove() }
    }

    override suspend fun getDriverById(driverId: String): Result<Driver> = try {
        val doc = firestore.collection("drivers").document(driverId).get().await()
        if (doc.exists()) {
            Result.Success(docToDriver(doc.id, doc.data))
        } else {
            Result.Error("Driver profile not found")
        }
    } catch (e: Exception) {
        Result.Error(e.message ?: "Failed to get driver details", throwable = e)
    }

    override suspend fun createDriver(driver: Driver): Result<Driver> = try {
        val docRef = if (driver.id.isNotBlank()) {
            firestore.collection("drivers").document(driver.id)
        } else {
            firestore.collection("drivers").document()
        }
        val driverWithId = driver.copy(id = docRef.id)
        docRef.set(driverToMap(driverWithId)).await()
        Result.Success(driverWithId)
    } catch (e: Exception) {
        Result.Error(e.message ?: "Failed to create driver", throwable = e)
    }

    override suspend fun updateDriver(driver: Driver): Result<Driver> = try {
        firestore.collection("drivers").document(driver.id).set(driverToMap(driver)).await()
        Result.Success(driver)
    } catch (e: Exception) {
        Result.Error(e.message ?: "Failed to update driver", throwable = e)
    }

    override suspend fun deleteDriver(driverId: String): Result<Boolean> = try {
        firestore.collection("drivers").document(driverId).delete().await()
        Result.Success(true)
    } catch (e: Exception) {
        Result.Error(e.message ?: "Failed to delete driver", throwable = e)
    }

    private fun docToDriver(id: String, data: Map<String, Any>?): Driver {
        val map = data ?: emptyMap()

        val uid = map["uid"] as? String ?: ""
        val name = map["name"] as? String ?: ""
        val email = map["email"] as? String ?: ""
        val phone = map["phone"] as? String ?: ""
        val role = map["role"] as? String ?: "DRIVER"
        val licenseNumber = map["licenseNumber"] as? String ?: ""
        
        val licenseExpiryDate = (map["licenseExpiryDate"] as? com.google.firebase.Timestamp)?.toDate()
            ?: (map["licenseExpiryDate"] as? Date) ?: Date()
            
        val statusStr = (map["status"] as? String) ?: "AVAILABLE"
        val status = try { DriverStatus.valueOf(statusStr.uppercase()) } catch (_: Exception) { DriverStatus.AVAILABLE }

        val assignedVehicle = map["assignedVehicle"] as? String
        val assignedVehicleId = map["assignedVehicleId"] as? String
        val rating = (map["rating"] as? Number)?.toDouble() ?: 5.0
        val totalDeliveries = (map["totalDeliveries"] as? Number)?.toInt() ?: 0
        val completedDeliveries = (map["completedDeliveries"] as? Number)?.toInt() ?: 0
        val profileImage = map["profileImage"] as? String
        val currentLatitude = (map["currentLatitude"] as? Number)?.toDouble() ?: 0.0
        val currentLongitude = (map["currentLongitude"] as? Number)?.toDouble() ?: 0.0
        val speed = (map["speed"] as? Number)?.toDouble() ?: 0.0
        val heading = (map["heading"] as? Number)?.toDouble() ?: 0.0
        val accuracy = (map["accuracy"] as? Number)?.toDouble() ?: 0.0
        val address = map["address"] as? String ?: ""
        val isActive = (map["isActive"] as? Boolean) ?: (status != DriverStatus.INACTIVE && status != DriverStatus.SUSPENDED)
        
        val joinedDate = (map["joinedDate"] as? com.google.firebase.Timestamp)?.toDate()
            ?: (map["joinedDate"] as? Date) ?: Date()
        val lastActive = (map["lastActive"] as? com.google.firebase.Timestamp)?.toDate()
            ?: (map["lastActive"] as? Date) ?: Date()
        val createdAt = (map["createdAt"] as? com.google.firebase.Timestamp)?.toDate()
            ?: (map["createdAt"] as? Date) ?: Date()

        return Driver(
            id = id,
            uid = uid,
            name = name,
            email = email,
            phone = phone,
            role = role,
            licenseNumber = licenseNumber,
            licenseExpiryDate = licenseExpiryDate,
            status = status,
            assignedVehicle = assignedVehicle,
            assignedVehicleId = assignedVehicleId,
            rating = rating,
            totalDeliveries = totalDeliveries,
            completedDeliveries = completedDeliveries,
            profileImage = profileImage,
            currentLatitude = currentLatitude,
            currentLongitude = currentLongitude,
            speed = speed,
            heading = heading,
            accuracy = accuracy,
            address = address,
            isActive = isActive,
            joinedDate = joinedDate,
            lastActive = lastActive,
            createdAt = createdAt
        )
    }

    private fun driverToMap(driver: Driver): Map<String, Any> {
        return mapOf(
            "uid" to driver.uid,
            "name" to driver.name,
            "email" to driver.email,
            "phone" to driver.phone,
            "role" to driver.role,
            "licenseNumber" to driver.licenseNumber,
            "licenseExpiryDate" to driver.licenseExpiryDate,
            "status" to driver.status.name.uppercase(),
            "assignedVehicle" to (driver.assignedVehicle ?: ""),
            "assignedVehicleId" to (driver.assignedVehicleId ?: ""),
            "rating" to driver.rating,
            "totalDeliveries" to driver.totalDeliveries,
            "completedDeliveries" to driver.completedDeliveries,
            "profileImage" to (driver.profileImage ?: ""),
            "currentLatitude" to driver.currentLatitude,
            "currentLongitude" to driver.currentLongitude,
            "speed" to driver.speed,
            "heading" to driver.heading,
            "accuracy" to driver.accuracy,
            "address" to driver.address,
            "isActive" to driver.isActive,
            "joinedDate" to driver.joinedDate,
            "lastActive" to driver.lastActive,
            "createdAt" to driver.createdAt
        )
    }
}
