package com.routecj.driver.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.routecj.driver.core.util.Result
import com.routecj.driver.data.mapper.DispatchMapper
import com.routecj.driver.domain.model.Dispatch
import com.routecj.driver.domain.model.DispatchStatus
import com.routecj.driver.domain.model.DriverStatus
import com.routecj.driver.domain.model.OrderStatus
import com.routecj.driver.domain.model.VehicleStatus
import com.routecj.driver.domain.repository.DispatchRepository
import com.routecj.driver.domain.usecase.VehicleAssignmentResolver
import com.routecj.driver.domain.usecase.VehicleResolutionResult
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.Date

/**
 * Firestore implementation of DispatchRepository.
 * Interacts with 'dispatches', 'orders', 'drivers', and 'vehicles' collections in RouteCJ Firebase.
 */
class FirestoreDispatchRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : DispatchRepository {

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

    override suspend fun getDispatchById(dispatchId: String): Result<Dispatch> = try {
        val doc = firestore.collection("dispatches").document(dispatchId).get().await()
        if (doc.exists()) {
            Result.Success(DispatchMapper.mapToDomain(doc.id, doc.data))
        } else {
            Result.Error("Dispatch not found")
        }
    } catch (e: Exception) {
        Result.Error(e.message ?: "Failed to fetch dispatch", e)
    }

    override fun observeDispatchById(dispatchId: String): Flow<Result<Dispatch>> = callbackFlow {
        val listener = firestore.collection("dispatches").document(dispatchId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Result.Error(error.message ?: "Error observing dispatch", error))
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    try {
                        val dispatch = DispatchMapper.mapToDomain(snapshot.id, snapshot.data)
                        trySend(Result.Success(dispatch))
                    } catch (e: Exception) {
                        trySend(Result.Error("Failed to parse dispatch document: ${e.message}", e))
                    }
                } else {
                    trySend(Result.Error("Dispatch record does not exist"))
                }
            }
        awaitClose { listener.remove() }
    }

    override fun observeAssignedDispatches(driverId: String): Flow<Result<List<Dispatch>>> = callbackFlow {
        val listener = firestore.collection("dispatches")
            .whereEqualTo("driverId", driverId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Result.Error(error.message ?: "Error observing assigned dispatches", error))
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val dispatches = snapshot.documents.mapNotNull { doc ->
                        try {
                            DispatchMapper.mapToDomain(doc.id, doc.data)
                        } catch (e: Exception) {
                            null
                        }
                    }
                    trySend(Result.Success(dispatches))
                }
            }
        awaitClose { listener.remove() }
    }

    /**
     * Atomically starts a trip for an assigned Dispatch.
     * Enforces Firestore transaction requirement: ALL READS MUST OCCUR BEFORE ALL WRITES.
     * Uses VehicleAssignmentResolver for single-source-of-truth canonical vehicle resolution.
     */
    override suspend fun startTrip(dispatchId: String, driverId: String): Result<Unit> = try {
        requireAuth(driverId)
        val vehicleRepo = FirestoreVehicleRepository(firestore)
        val resolver = VehicleAssignmentResolver(vehicleRepo)

        // 1. Fetch dispatch and driver records BEFORE transaction
        val dispatchSnapshot = firestore.collection("dispatches").document(dispatchId).get().await()
        if (!dispatchSnapshot.exists()) {
            throw Exception("TRIP NOT READY: Dispatch record not found.")
        }

        val driverSnapshot = firestore.collection("drivers").document(driverId).get().await()
        if (!driverSnapshot.exists()) {
            throw Exception("TRIP NOT READY: Driver record not found.")
        }

        // Extract vehicle references independently
        val tripVehicleRef = dispatchSnapshot.getString("vehicleId")
            ?: dispatchSnapshot.getString("assignedVehicleId")
            ?: dispatchSnapshot.getString("vehicleRegistration")
            ?: dispatchSnapshot.getString("vehicleNumber")

        val driverVehicleRef = driverSnapshot.getString("assignedVehicleId")
            ?: driverSnapshot.getString("assignedVehicle")
            ?: driverSnapshot.getString("vehicleRegistration")
            ?: driverSnapshot.getString("vehicleNumber")
            ?: driverSnapshot.getString("vehicleId")

        // 2. Resolve vehicle assignment using VehicleAssignmentResolver
        val resolution = resolver.resolve(
            tripVehicleRef = tripVehicleRef,
            driverVehicleRef = driverVehicleRef
        )

        val assignment = when (resolution) {
            is VehicleResolutionResult.Success -> resolution.assignment
            is VehicleResolutionResult.NoVehicleReference -> {
                throw Exception("NO_VEHICLE_REFERENCE: No vehicle is assigned to this trip or driver.")
            }
            is VehicleResolutionResult.RecordNotFound -> {
                throw Exception("VEHICLE_RECORD_NOT_FOUND: Vehicle record (${resolution.reference}) could not be found.")
            }
            is VehicleResolutionResult.VehicleMismatch -> {
                throw Exception("VEHICLE_MISMATCH: Trip vehicle (${resolution.tripVehicleId}) does not match driver's assigned vehicle (${resolution.driverVehicleId}).")
            }
        }

        val effectiveVehicleId = assignment.canonicalVehicleId

        firestore.runTransaction { transaction ->
            // ==========================================
            // 1. ALL READS FIRST
            // ==========================================
            val dispatchRef = firestore.collection("dispatches").document(dispatchId)
            val driverRef = firestore.collection("drivers").document(driverId)

            val dispatchDoc = transaction.get(dispatchRef)
            val driverDoc = transaction.get(driverRef)

            val orderId = dispatchDoc.getString("orderId")
            val orderRef = if (!orderId.isNullOrBlank()) firestore.collection("orders").document(orderId) else null
            val orderDoc = orderRef?.let { transaction.get(it) }

            val vehicleRef = firestore.collection("vehicles").document(effectiveVehicleId)
            val vehicleDoc = transaction.get(vehicleRef)

            // ==========================================
            // 2. VALIDATIONS AFTER ALL READS
            // ==========================================
            if (!dispatchDoc.exists()) {
                throw Exception("TRIP NOT READY: Dispatch record not found.")
            }
            if (!driverDoc.exists()) {
                throw Exception("TRIP NOT READY: Driver record not found.")
            }

            val authUid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
            val assignedDriverId = dispatchDoc.getString("driverId") ?: dispatchDoc.getString("assignedDriverId")
            if (!assignedDriverId.isNullOrBlank() && assignedDriverId != driverId && assignedDriverId != authUid) {
                throw Exception("TRIP ACCESS DENIED: Unauthorized driver.")
            }

            if (!vehicleDoc.exists()) {
                throw Exception("VEHICLE_RECORD_NOT_FOUND: Vehicle record ($effectiveVehicleId) not found in database.")
            }

            val vStatus = vehicleDoc.getString("status")?.uppercase()
            if (vStatus == "MAINTENANCE" || vStatus == "INACTIVE") {
                throw Exception("TRIP NOT READY: Assigned vehicle is currently $vStatus.")
            }

            val currentStatus = dispatchDoc.getString("status")?.uppercase() ?: ""
            if (currentStatus in listOf("TRIP_STARTED", "IN_TRANSIT", "DELIVERED", "COMPLETED")) {
                throw Exception("TRIP NOT READY: Trip is already active or delivered.")
            }

            val now = Date()

            // ==========================================
            // 3. ALL WRITES AFTER ALL READS
            // ==========================================
            // A. Update Dispatch Status
            transaction.update(
                dispatchRef,
                mapOf(
                    "status" to DispatchStatus.TRIP_STARTED.name,
                    "vehicleId" to effectiveVehicleId,
                    "updatedAt" to now
                )
            )

            // B. Update linked Order Status
            if (orderRef != null && orderDoc != null && orderDoc.exists()) {
                transaction.update(
                    orderRef,
                    mapOf(
                        "status" to OrderStatus.DISPATCHED.name,
                        "updatedAt" to now
                    )
                )
            }

            // C. Update Driver Status to ON_DUTY
            transaction.update(
                driverRef,
                mapOf(
                    "status" to DriverStatus.ON_DUTY.name.lowercase(),
                    "lastActive" to now
                )
            )

            // D. Update Vehicle Status to IN_TRANSIT
            transaction.update(
                vehicleRef,
                mapOf(
                    "status" to VehicleStatus.IN_TRANSIT.name,
                    "updatedAt" to now
                )
            )
            null
        }.await()
        Result.Success(Unit)
    } catch (e: Exception) {
        val msg = e.message ?: "Failed to start trip"
        Result.Error(msg, e)
    }

    override suspend fun completeTrip(dispatchId: String, driverId: String): Result<Unit> = try {
        requireAuth(driverId)
        val dispatchRef = firestore.collection("dispatches").document(dispatchId)
        
        val initialDispatchDoc = dispatchRef.get().await()
        if (!initialDispatchDoc.exists()) throw Exception("TRIP NOT FOUND: Dispatch record not found.")

        val dispatchVehicleId = initialDispatchDoc.getString("vehicleId") 
            ?: initialDispatchDoc.getString("assignedVehicleId")
            ?: initialDispatchDoc.getString("vehicleRegistration")
        val orderId = initialDispatchDoc.getString("orderId") ?: ""
        
        val driverRef = firestore.collection("drivers").document(driverId)
        val driverLocationsRef = firestore.collection("driverLocations").document(driverId)

        val vehicleRepo = FirestoreVehicleRepository(firestore)
        val canonicalVehicleId = if (!dispatchVehicleId.isNullOrBlank()) {
            val res = vehicleRepo.getVehicleById(dispatchVehicleId)
            if (res is Result.Success) res.data.id else null
        } else null

        firestore.runTransaction { transaction ->
            val dispatchDoc = transaction.get(dispatchRef)
            
            if (dispatchDoc.getString("driverId") != driverId) {
                throw Exception("TRIP ACCESS DENIED: Unauthorized driver.")
            }

            val currentStatus = dispatchDoc.getString("status")?.uppercase()
            if (currentStatus == "DELIVERED" || currentStatus == "COMPLETED") {
                throw Exception("TRIP COMPLETED: Trip is already delivered.")
            }

            // Apply Writes
            transaction.update(dispatchRef, mapOf(
                "status" to "DELIVERED",
                "updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
            ))

            if (orderId.isNotBlank()) {
                val orderRef = firestore.collection("orders").document(orderId)
                transaction.update(orderRef, mapOf(
                    "status" to "DELIVERED",
                    "deliveredAt" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
                    "deliveredByUid" to driverId,
                    "updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                ))
            }

            transaction.update(driverRef, mapOf(
                "status" to "AVAILABLE",
                "isLocationSharing" to false,
                "activeTripId" to null,
                "updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
            ))

            // Set Location sharing to false
            transaction.update(driverLocationsRef, mapOf(
                "isLocationSharing" to false,
                "activeTripId" to null,
                "timestamp" to com.google.firebase.firestore.FieldValue.serverTimestamp()
            ))

            if (canonicalVehicleId != null) {
                val vehicleRef = firestore.collection("vehicles").document(canonicalVehicleId)
                transaction.update(vehicleRef, mapOf(
                    "status" to "AVAILABLE",
                    "updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                ))
            }
        }.await()
        Result.Success(Unit)
    } catch (e: Exception) {
        val msg = e.message ?: "Failed to complete trip"
        val displayMsg = if (msg.contains("TRIP")) msg else "Unable to complete trip. Please try again."
        Result.Error(displayMsg, e)
    }
}
