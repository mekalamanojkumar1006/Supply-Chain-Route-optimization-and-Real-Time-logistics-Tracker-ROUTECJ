package com.routecj.driver.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.routecj.driver.core.util.Result
import com.routecj.driver.data.mapper.VehicleMapper
import com.routecj.driver.domain.model.Vehicle
import com.routecj.driver.domain.repository.VehicleRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Firestore implementation of VehicleRepository.
 * Interacts with 'vehicles' collection in RouteCJ Firebase.
 * Supports canonical vehicle resolution across document ID, registrationNumber,
 * vehicleNumber, vehicleRegistration, assignedVehicleId, assignedDriverId, and driverId.
 */
class FirestoreVehicleRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : VehicleRepository {

    override suspend fun getVehicleById(vehicleId: String): Result<Vehicle> {
        val trimmedRef = vehicleId.trim()
        if (trimmedRef.isBlank()) {
            return Result.Error("Vehicle reference is empty")
        }

        return try {
            // 1. Direct Document ID lookup
            val doc = firestore.collection("vehicles").document(trimmedRef).get().await()
            if (doc.exists()) {
                return Result.Success(VehicleMapper.mapToDomain(doc.id, doc.data))
            }

            // 2. Query fields in sequence to resolve canonical vehicle document
            val fieldNames = listOf(
                "registrationNumber",
                "vehicleNumber",
                "vehicleRegistration",
                "assignedVehicleId",
                "assignedDriverId",
                "driverId"
            )

            for (field in fieldNames) {
                val query = firestore.collection("vehicles")
                    .whereEqualTo(field, trimmedRef)
                    .limit(1)
                    .get()
                    .await()
                if (!query.isEmpty) {
                    val qDoc = query.documents[0]
                    return Result.Success(VehicleMapper.mapToDomain(qDoc.id, qDoc.data))
                }
            }

            Result.Error("VEHICLE_RECORD_NOT_FOUND: Vehicle record ($trimmedRef) could not be found.")
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to fetch vehicle", e)
        }
    }

    override fun observeVehicleById(vehicleId: String): Flow<Result<Vehicle>> = callbackFlow {
        val trimmedRef = vehicleId.trim()
        if (trimmedRef.isBlank()) {
            trySend(Result.Error("Vehicle reference is empty"))
            close()
            return@callbackFlow
        }

        val listeners = mutableListOf<com.google.firebase.firestore.ListenerRegistration>()

        // Observe direct document ID first
        val docListener = firestore.collection("vehicles").document(trimmedRef)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Result.Error(error.message ?: "Error observing vehicle", error))
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    try {
                        val vehicle = VehicleMapper.mapToDomain(snapshot.id, snapshot.data)
                        trySend(Result.Success(vehicle))
                    } catch (e: Exception) {
                        trySend(Result.Error("Failed to parse vehicle document: ${e.message}", e))
                    }
                } else {
                    // Fallback query across vehicle fields
                    val fieldNames = listOf(
                        "registrationNumber",
                        "vehicleNumber",
                        "vehicleRegistration",
                        "assignedVehicleId",
                        "assignedDriverId",
                        "driverId"
                    )

                    fun tryNextField(index: Int) {
                        if (index >= fieldNames.size) {
                            trySend(Result.Error("VEHICLE_RECORD_NOT_FOUND: Vehicle document does not exist"))
                            return
                        }

                        val field = fieldNames[index]
                        val qListener = firestore.collection("vehicles")
                            .whereEqualTo(field, trimmedRef)
                            .limit(1)
                            .addSnapshotListener { querySnapshot, queryError ->
                                if (queryError != null) {
                                    tryNextField(index + 1)
                                    return@addSnapshotListener
                                }
                                if (querySnapshot != null && !querySnapshot.isEmpty) {
                                    val queryDoc = querySnapshot.documents[0]
                                    try {
                                        val vehicle = VehicleMapper.mapToDomain(queryDoc.id, queryDoc.data)
                                        trySend(Result.Success(vehicle))
                                    } catch (e: Exception) {
                                        trySend(Result.Error("Failed to parse vehicle document: ${e.message}", e))
                                    }
                                } else {
                                    tryNextField(index + 1)
                                }
                            }
                        listeners.add(qListener)
                    }

                    tryNextField(0)
                }
            }
        listeners.add(docListener)

        awaitClose {
            listeners.forEach { it.remove() }
        }
    }
}
