package com.routecj.admin.data.repository

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.routecj.admin.core.util.Result
import com.routecj.admin.domain.model.Vehicle
import com.routecj.admin.domain.model.VehicleStatus
import com.routecj.admin.domain.model.VehicleType
import com.routecj.admin.domain.repository.VehicleRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.Date
import javax.inject.Inject
import com.routecj.admin.domain.model.VehicleLog
import com.google.firebase.storage.FirebaseStorage

/**
 * Firestore implementation of VehicleRepository.
 * Handles realtime tracking and sync with the Firestore 'vehicles' collection.
 */
class FirestoreVehicleRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage,
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: android.content.Context
) : VehicleRepository {

    override suspend fun getAllVehicles(): Flow<Result<List<Vehicle>>> = callbackFlow {
        val listener = firestore.collection("vehicles")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Result.Error(error.message ?: "Failed to listen to vehicles", throwable = error))
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val vehiclesList = snapshot.documents.mapNotNull { doc ->
                        try {
                            docToVehicle(doc.id, doc.data)
                        } catch (e: Exception) {
                            Log.e("FIRESTORE_VEHICLE_REPO", "Error parsing vehicle ${doc.id}", e)
                            null
                        }
                    }
                    trySend(Result.Success(vehiclesList))
                }
            }
        awaitClose { listener.remove() }
    }

    override suspend fun getVehicleById(vehicleId: String): Result<Vehicle> = try {
        val doc = firestore.collection("vehicles").document(vehicleId).get().await()
        if (doc.exists()) {
            Result.Success(docToVehicle(doc.id, doc.data))
        } else {
            Result.Error("Vehicle profile not found")
        }
    } catch (e: Exception) {
        Result.Error(e.message ?: "Failed to get vehicle details", throwable = e)
    }

    override suspend fun createVehicle(vehicle: Vehicle): Result<Vehicle> = try {
        val docRef = if (vehicle.id.isNotBlank()) {
            firestore.collection("vehicles").document(vehicle.id)
        } else {
            firestore.collection("vehicles").document()
        }
        val vehicleWithId = vehicle.copy(id = docRef.id)
        docRef.set(vehicleToMap(vehicleWithId)).await()
        Result.Success(vehicleWithId)
    } catch (e: Exception) {
        Result.Error(e.message ?: "Failed to create vehicle", throwable = e)
    }

    override suspend fun updateVehicle(vehicle: Vehicle): Result<Vehicle> = try {
        firestore.collection("vehicles").document(vehicle.id).set(vehicleToMap(vehicle)).await()
        Result.Success(vehicle)
    } catch (e: Exception) {
        Result.Error(e.message ?: "Failed to update vehicle", throwable = e)
    }

    override suspend fun deleteVehicle(vehicleId: String): Result<Boolean> = try {
        firestore.collection("vehicles").document(vehicleId).delete().await()
        Result.Success(true)
    } catch (e: Exception) {
        Result.Error(e.message ?: "Failed to delete vehicle", throwable = e)
    }

    override suspend fun getVehicleLogs(vehicleId: String): Flow<Result<List<VehicleLog>>> = callbackFlow {
        val listener = firestore.collection("vehicles").document(vehicleId).collection("logs")
            .orderBy("date", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Result.Error(error.message ?: "Failed to fetch vehicle logs", throwable = error))
                    return@addSnapshotListener
                }
                
                if (snapshot != null) {
                    val logs = snapshot.documents.mapNotNull { doc ->
                        try {
                            docToVehicleLog(doc.id, doc.data, vehicleId)
                        } catch (e: Exception) {
                            Log.e("FIRESTORE_VEHICLE_REPO", "Error parsing vehicle log ${doc.id}", e)
                            null
                        }
                    }
                    trySend(Result.Success(logs))
                }
            }
        awaitClose { listener.remove() }
    }

    override suspend fun uploadVehicleImage(vehicleId: String, imageUri: android.net.Uri): Result<String> = try {
        Log.d("VEHICLE_IMAGE_UPLOAD", "Starting upload for vehicleId='$vehicleId', uri='$imageUri'")

        // 1. Ensure valid document ID
        val cleanVehicleId = vehicleId.trim().ifBlank { firestore.collection("vehicles").document().id }
        val storageVehicleId = cleanVehicleId.filter { it.isLetterOrDigit() || it == '-' || it == '_' }
        val filename = "vehicle_${storageVehicleId}_${System.currentTimeMillis()}.jpg"
        val storagePath = "vehicles/$storageVehicleId/$filename"

        Log.d("VEHICLE_IMAGE_UPLOAD", "Configured storage path: $storagePath")

        // 2. Safely check for and clean up previous image (optional, won't break on failure)
        try {
            val vehicleDoc = firestore.collection("vehicles").document(cleanVehicleId).get().await()
            if (vehicleDoc.exists()) {
                val oldImageUrl = vehicleDoc.getString("imageUrl")
                if (!oldImageUrl.isNullOrBlank()) {
                    Log.d("VEHICLE_IMAGE_UPLOAD", "Cleaning up previous image: $oldImageUrl")
                    storage.getReferenceFromUrl(oldImageUrl).delete().await()
                }
            }
        } catch (e: Exception) {
            Log.w("VEHICLE_IMAGE_UPLOAD", "Previous image cleanup skipped or failed: ${e.message}")
        }

        // 3. Open InputStream via ContentResolver & detect MIME type
        val contentResolver = context.contentResolver
        val rawMimeType = contentResolver.getType(imageUri)
        val mimeType = if (!rawMimeType.isNullOrBlank() && rawMimeType.startsWith("image/")) rawMimeType else "image/jpeg"

        Log.d("VEHICLE_IMAGE_UPLOAD", "Opening ContentResolver stream. Detected MIME type: $mimeType")
        val inputStream = contentResolver.openInputStream(imageUri)
            ?: throw IllegalStateException("Could not open ContentResolver stream for image URI: $imageUri")

        val imageBytes = inputStream.use { it.readBytes() }
        if (imageBytes.isEmpty()) {
            throw IllegalStateException("Selected image file contains 0 bytes.")
        }

        Log.d("VEHICLE_IMAGE_UPLOAD", "Read ${imageBytes.size} bytes from image stream.")

        // 4. Create explicit StorageMetadata (Crucial for storage rules validation)
        val metadata = com.google.firebase.storage.StorageMetadata.Builder()
            .setContentType(mimeType)
            .setCustomMetadata("vehicleId", cleanVehicleId)
            .build()

        // 5. Perform Storage Upload with explicit metadata
        val storageRef = storage.reference.child("vehicles").child(storageVehicleId).child(filename)
        Log.d("VEHICLE_IMAGE_UPLOAD", "Uploading byte array (${imageBytes.size} bytes) to path: ${storageRef.path}")

        val uploadTask = storageRef.putBytes(imageBytes, metadata).await()
        Log.d("VEHICLE_IMAGE_UPLOAD", "Upload complete: bytesTransferred=${uploadTask.bytesTransferred}")

        // 6. Get Download URL
        val downloadUrl = storageRef.downloadUrl.await().toString()
        Log.d("VEHICLE_IMAGE_UPLOAD", "Download URL generated: $downloadUrl")

        // 7. Update Firestore Vehicle Document using SetOptions.merge()
        val updateMap = mapOf(
            "imageUrl" to downloadUrl,
            "updatedAt" to Date()
        )
        firestore.collection("vehicles").document(cleanVehicleId)
            .set(updateMap, com.google.firebase.firestore.SetOptions.merge())
            .await()

        Log.d("VEHICLE_IMAGE_UPLOAD", "Firestore vehicle document updated successfully for vehicleId='$cleanVehicleId'")

        Result.Success(downloadUrl)
    } catch (e: com.google.firebase.storage.StorageException) {
        val userError = when (e.errorCode) {
            com.google.firebase.storage.StorageException.ERROR_NOT_AUTHORIZED -> "Firebase Storage permission denied."
            com.google.firebase.storage.StorageException.ERROR_NOT_AUTHENTICATED -> "User session expired. Please re-login."
            com.google.firebase.storage.StorageException.ERROR_BUCKET_NOT_FOUND -> "Firebase Storage is not enabled or bucket was not found for this project."
            com.google.firebase.storage.StorageException.ERROR_RETRY_LIMIT_EXCEEDED -> "Upload network timeout. Please check your connection."
            else -> e.message ?: "Firebase Storage error [Code ${e.errorCode}]"
        }
        val storagePath = "vehicles/$vehicleId"
        Log.e(
            "VEHICLE_IMAGE_UPLOAD",
            "StorageException for vehicleId='$vehicleId', path='$storagePath', errorCode=${e.errorCode}, message='${e.message}'",
            e
        )
        Result.Error("Vehicle image upload failed: $userError", throwable = e)
    } catch (e: Exception) {
        val storagePath = "vehicles/$vehicleId"
        Log.e(
            "VEHICLE_IMAGE_UPLOAD",
            "Unexpected error uploading vehicle image for vehicleId='$vehicleId', path='$storagePath', message='${e.message}'",
            e
        )
        Result.Error("Vehicle image upload failed: ${e.localizedMessage ?: e.message ?: "Unknown error"}", throwable = e)
    }

    private fun docToVehicleLog(id: String, data: Map<String, Any>?, vehicleId: String): VehicleLog {
        val map = data ?: emptyMap()
        val vehicleNumber = map["vehicleNumber"] as? String ?: ""
        val driverName = map["driverName"] as? String ?: ""
        val gateNumber = map["gateNumber"] as? String ?: ""
        val remarks = map["remarks"] as? String
        
        val timeIn = (map["timeIn"] as? com.google.firebase.Timestamp)?.toDate()
            ?: (map["timeIn"] as? Date)
        val timeOut = (map["timeOut"] as? com.google.firebase.Timestamp)?.toDate()
            ?: (map["timeOut"] as? Date)
        val date = (map["date"] as? com.google.firebase.Timestamp)?.toDate()
            ?: (map["date"] as? Date) ?: Date()
            
        return VehicleLog(
            id = id,
            vehicleId = vehicleId,
            vehicleNumber = vehicleNumber,
            driverName = driverName,
            timeIn = timeIn,
            timeOut = timeOut,
            gateNumber = gateNumber,
            date = date,
            remarks = remarks
        )
    }

    private fun docToVehicle(id: String, data: Map<String, Any>?): Vehicle {
        val map = data ?: emptyMap()

        val vehicleNumber = map["vehicleNumber"] as? String ?: ""
        
        val typeStr = (map["vehicleType"] as? String) ?: "VAN"
        val vehicleType = try { VehicleType.valueOf(typeStr.uppercase()) } catch (_: Exception) { VehicleType.VAN }
        
        val brand = map["brand"] as? String ?: ""
        val model = map["model"] as? String ?: ""
        val registrationNumber = map["registrationNumber"] as? String ?: ""
        
        val driverId = map["driverId"] as? String
        val driverName = map["driverName"] as? String ?: ""
        
        val capacity = (map["capacity"] as? Number)?.toDouble() ?: 0.0
        val capacityUnit = map["capacityUnit"] as? String ?: "tons"
        val imageUrl = map["imageUrl"] as? String
        val fuelLevel = (map["fuelLevel"] as? Number)?.toDouble() ?: 100.0
        
        val statusStr = (map["status"] as? String) ?: "AVAILABLE"
        val status = try { VehicleStatus.valueOf(statusStr.uppercase()) } catch (_: Exception) { VehicleStatus.AVAILABLE }

        val lastServiceDate = (map["lastServiceDate"] as? com.google.firebase.Timestamp)?.toDate()
            ?: (map["lastServiceDate"] as? Date) ?: Date()
        val nextServiceDate = (map["nextServiceDate"] as? com.google.firebase.Timestamp)?.toDate()
            ?: (map["nextServiceDate"] as? Date) ?: Date()
        val insuranceExpiry = (map["insuranceExpiry"] as? com.google.firebase.Timestamp)?.toDate()
            ?: (map["insuranceExpiry"] as? Date) ?: Date()

        val currentLatitude = (map["currentLatitude"] as? Number)?.toDouble() ?: 0.0
        val currentLongitude = (map["currentLongitude"] as? Number)?.toDouble() ?: 0.0
        val speed = (map["speed"] as? Number)?.toDouble() ?: 0.0
        val location = map["location"] as? String ?: ""
        val odometer = (map["odometer"] as? Number)?.toDouble() ?: 0.0

        val createdAt = (map["createdAt"] as? com.google.firebase.Timestamp)?.toDate()
            ?: (map["createdAt"] as? Date) ?: Date()
        val updatedAt = (map["updatedAt"] as? com.google.firebase.Timestamp)?.toDate()
            ?: (map["updatedAt"] as? Date) ?: Date()

        return Vehicle(
            id = id,
            vehicleNumber = vehicleNumber,
            vehicleType = vehicleType,
            brand = brand,
            model = model,
            registrationNumber = registrationNumber,
            driverId = driverId,
            driverName = driverName,
            capacity = capacity,
            capacityUnit = capacityUnit,
            imageUrl = imageUrl,
            fuelLevel = fuelLevel,
            status = status,
            lastServiceDate = lastServiceDate,
            nextServiceDate = nextServiceDate,
            insuranceExpiry = insuranceExpiry,
            currentLatitude = currentLatitude,
            currentLongitude = currentLongitude,
            speed = speed,
            location = location,
            odometer = odometer,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }

    private fun vehicleToMap(vehicle: Vehicle): Map<String, Any> {
        return mapOf(
            "vehicleNumber" to vehicle.vehicleNumber,
            "vehicleType" to vehicle.vehicleType.name.lowercase(),
            "brand" to vehicle.brand,
            "model" to vehicle.model,
            "registrationNumber" to vehicle.registrationNumber,
            "driverId" to (vehicle.driverId ?: ""),
            "driverName" to vehicle.driverName,
            "capacity" to vehicle.capacity,
            "capacityUnit" to vehicle.capacityUnit,
            "imageUrl" to (vehicle.imageUrl ?: ""),
            "fuelLevel" to vehicle.fuelLevel,
            "status" to vehicle.status.name.lowercase(),
            "lastServiceDate" to vehicle.lastServiceDate,
            "nextServiceDate" to vehicle.nextServiceDate,
            "insuranceExpiry" to vehicle.insuranceExpiry,
            "currentLatitude" to vehicle.currentLatitude,
            "currentLongitude" to vehicle.currentLongitude,
            "speed" to vehicle.speed,
            "location" to vehicle.location,
            "odometer" to vehicle.odometer,
            "createdAt" to vehicle.createdAt,
            "updatedAt" to vehicle.updatedAt
        )
    }
}
