package com.routecj.admin.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.routecj.admin.core.util.Result
import com.routecj.admin.domain.model.Godown
import com.routecj.admin.domain.model.GodownStatus
import com.routecj.admin.domain.repository.GodownRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.Date
import javax.inject.Inject

/**
 * Firestore implementation of GodownRepository.
 */
class FirestoreGodownRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) : GodownRepository {

    private val collection = firestore.collection("godowns")

    override suspend fun getAllGodowns(): Flow<Result<List<Godown>>> = callbackFlow {
        val listener = collection.addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(Result.Error(error.message ?: "Failed to listen to godowns"))
                return@addSnapshotListener
            }
            if (snapshot != null) {
                var godowns = snapshot.documents.mapNotNull { doc ->
                    docToGodown(doc.id, doc.data)
                }
                
                // Ensure Vizianagaram Store exists in the list (Database-driven fallback)
                if (godowns.none { it.name.contains("Vizianagaram", ignoreCase = true) }) {
                    godowns = godowns + getBuiltInMainStore()
                }
                
                trySend(Result.Success(godowns))
            }
        }
        awaitClose { listener.remove() }
    }

    private fun getBuiltInMainStore(): Godown {
        return Godown(
            id = "STORE_VZM_MAIN",
            name = "RouteCJ Vizianagaram Store",
            address = "Vizianagaram Bus Complex",
            city = "Vizianagaram",
            state = "Andhra Pradesh",
            pincode = "535003",
            latitude = 18.1124436,
            longitude = 83.3986427,
            capacity = 5000.0,
            currentStock = 1200.0,
            status = GodownStatus.ACTIVE
        )
    }

    override suspend fun getGodownById(id: String): Result<Godown> = try {
        if (id == "STORE_VZM_MAIN") {
            Result.Success(getBuiltInMainStore())
        } else {
            val doc = collection.document(id).get().await()
            if (doc.exists()) {
                Result.Success(docToGodown(doc.id, doc.data)!!)
            } else {
                Result.Error("Godown not found")
            }
        }
    } catch (e: Exception) {
        Result.Error(e.message ?: "Error getting godown")
    }

    override suspend fun createGodown(godown: Godown): Result<Unit> = try {
        val docRef = collection.document()
        val data = godownToMap(godown.copy(id = docRef.id))
        docRef.set(data).await()
        Result.Success(Unit)
    } catch (e: Exception) {
        Result.Error(e.message ?: "Error creating godown")
    }

    override suspend fun updateGodown(godown: Godown): Result<Unit> = try {
        val data = godownToMap(godown)
        collection.document(godown.id).set(data).await()
        Result.Success(Unit)
    } catch (e: Exception) {
        Result.Error(e.message ?: "Error updating godown")
    }

    override suspend fun deleteGodown(id: String): Result<Unit> = try {
        collection.document(id).delete().await()
        Result.Success(Unit)
    } catch (e: Exception) {
        Result.Error(e.message ?: "Error deleting godown")
    }

    private fun docToGodown(id: String, data: Map<String, Any>?): Godown? {
        if (data == null) return null
        val statusStr = (data["status"] as? String)?.uppercase() ?: "ACTIVE"
        val status = try { GodownStatus.valueOf(statusStr) } catch (_: Exception) { GodownStatus.ACTIVE }
        
        return Godown(
            id = id,
            name = data["godownName"] as? String ?: data["name"] as? String ?: "",
            address = data["address"] as? String ?: "",
            city = data["city"] as? String ?: "",
            state = data["state"] as? String ?: "",
            pincode = data["pincode"] as? String ?: "",
            latitude = (data["latitude"] as? Number)?.toDouble() ?: 0.0,
            longitude = (data["longitude"] as? Number)?.toDouble() ?: 0.0,
            capacity = (data["capacity"] as? Number)?.toDouble() ?: 0.0,
            currentStock = (data["currentStock"] as? Number)?.toDouble() ?: 0.0,
            managerId = data["managerId"] as? String,
            managerName = data["managerName"] as? String,
            phone = data["phone"] as? String ?: "",
            status = status,
            createdAt = (data["createdAt"] as? com.google.firebase.Timestamp)?.toDate() ?: Date(),
            updatedAt = (data["updatedAt"] as? com.google.firebase.Timestamp)?.toDate() ?: Date()
        )
    }

    private fun godownToMap(godown: Godown): Map<String, Any?> {
        return mapOf(
            "godownName" to godown.name,
            "address" to godown.address,
            "city" to godown.city,
            "state" to godown.state,
            "pincode" to godown.pincode,
            "latitude" to godown.latitude,
            "longitude" to godown.longitude,
            "capacity" to godown.capacity,
            "currentStock" to godown.currentStock,
            "managerId" to godown.managerId,
            "managerName" to godown.managerName,
            "phone" to godown.phone,
            "status" to godown.status.name,
            "createdAt" to godown.createdAt,
            "updatedAt" to godown.updatedAt
        )
    }
}
