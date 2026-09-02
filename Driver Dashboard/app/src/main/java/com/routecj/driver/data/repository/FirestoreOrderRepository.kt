package com.routecj.driver.data.repository

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.routecj.driver.core.util.Result
import com.routecj.driver.data.mapper.OrderMapper
import com.routecj.driver.domain.model.DriverStatus
import com.routecj.driver.domain.model.Order
import com.routecj.driver.domain.model.OrderStatus
import com.routecj.driver.domain.model.VehicleStatus
import com.routecj.driver.domain.repository.OrderRepository
import com.routecj.driver.domain.usecase.VehicleAssignmentResolver
import com.routecj.driver.domain.usecase.VehicleResolutionResult
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.Date

/**
 * Firestore implementation of OrderRepository.
 * Interacts with 'orders', 'drivers', and 'vehicles' collections in RouteCJ Firebase.
 */
class FirestoreOrderRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : OrderRepository {

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

    override suspend fun getOrderById(orderId: String): Result<Order> = try {
        val doc = firestore.collection("orders").document(orderId).get().await()
        if (doc.exists()) {
            Result.Success(OrderMapper.mapToDomain(doc.id, doc.data))
        } else {
            Result.Error("Order not found")
        }
    } catch (e: Exception) {
        Result.Error(e.message ?: "Failed to fetch order", e)
    }

    override fun observeOrderById(orderId: String): Flow<Result<Order>> = callbackFlow {
        val listener = firestore.collection("orders").document(orderId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Result.Error(error.message ?: "Error observing order", error))
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    try {
                        val order = OrderMapper.mapToDomain(snapshot.id, snapshot.data)
                        trySend(Result.Success(order))
                    } catch (e: Exception) {
                        trySend(Result.Error("Failed to parse order document: ${e.message}", e))
                    }
                } else {
                    trySend(Result.Error("Order record does not exist"))
                }
            }
        awaitClose { listener.remove() }
    }

    override fun observeAssignedOrders(driverId: String): Flow<Result<List<Order>>> = callbackFlow {
        val listener = firestore.collection("orders")
            .whereEqualTo("assignedDriverId", driverId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Result.Error(error.message ?: "Error observing assigned orders", error))
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val orders = snapshot.documents.mapNotNull { doc ->
                        try {
                            OrderMapper.mapToDomain(doc.id, doc.data)
                        } catch (e: Exception) {
                            null
                        }
                    }
                    trySend(Result.Success(orders))
                }
            }
        awaitClose { listener.remove() }
    }

    override fun observeBookedPickups(driverId: String): Flow<Result<List<Order>>> = callbackFlow {
        val listener = firestore.collection("orders")
            .whereEqualTo("assignedDriverId", driverId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Result.Error(error.message ?: "Error observing booked pickups", error))
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val pickups = snapshot.documents.mapNotNull { doc ->
                        try {
                            val order = OrderMapper.mapToDomain(doc.id, doc.data)
                            val s = order.status.name.uppercase()
                            if (s in listOf("ASSIGNED", "PENDING", "PICKED_UP", "PENDING_GODOWN_REVIEW") && !order.otpVerified) {
                                order
                            } else null
                        } catch (e: Exception) {
                            null
                        }
                    }
                    trySend(Result.Success(pickups))
                }
            }
        awaitClose { listener.remove() }
    }

    override suspend fun markDriverArrived(orderId: String, driverId: String): Result<Unit> = try {
        requireAuth(driverId)
        firestore.runTransaction { transaction ->
            val orderRef = firestore.collection("orders").document(orderId)
            val orderDoc = transaction.get(orderRef)

            if (!orderDoc.exists()) {
                throw Exception("Booking / Order #$orderId not found.")
            }

            val assignedDriver = orderDoc.getString("assignedDriverId") ?: orderDoc.getString("driverId")
            if (assignedDriver != driverId) {
                throw Exception("Unauthorized: This pickup is assigned to another driver.")
            }

            val now = Date()

            transaction.update(
                orderRef,
                mapOf(
                    "driverArrived" to true,
                    "driverArrivedAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to now
                )
            )
        }.await()
        Result.Success(Unit)
    } catch (e: Exception) {
        Result.Error(e.message ?: "Failed to mark driver arrival", e)
    }

    override suspend fun verifyPickupOtp(orderId: String, enteredOtp: String, driverId: String): Result<Unit> = try {
        requireAuth(driverId)
        firestore.runTransaction { transaction ->
            val orderRef = firestore.collection("orders").document(orderId)
            val orderDoc = transaction.get(orderRef)

            if (!orderDoc.exists()) {
                throw Exception("Booking / Order #$orderId not found.")
            }

            val assignedDriver = orderDoc.getString("assignedDriverId") ?: orderDoc.getString("driverId")
            if (assignedDriver != driverId) {
                throw Exception("TRIP ACCESS DENIED: You are not authorized for this pickup.")
            }

            val driverArrived = orderDoc.getBoolean("driverArrived") ?: false
            if (!driverArrived) {
                throw Exception("Please confirm your arrival before verifying the pickup OTP.")
            }

            val isAlreadyVerified = orderDoc.getBoolean("otpVerified") ?: false
            if (isAlreadyVerified) {
                throw Exception("OTP ALREADY VERIFIED: This pickup has already been verified.")
            }

            val storedOtp = orderDoc.getString("pickupOtp")
                ?: orderDoc.getString("otp")
                ?: orderDoc.getString("customerOtp")
                ?: ""

            if (storedOtp.isBlank()) {
                throw Exception("Customer has not yet generated the pickup OTP. Please ask the customer to view their booking in the RouteCJ app.")
            }

            val expiresAt = (orderDoc.get("pickupOtpExpiresAt") as? com.google.firebase.Timestamp)?.toDate()
                ?: (orderDoc.get("otpExpiresAt") as? com.google.firebase.Timestamp)?.toDate()
            if (expiresAt != null && Date().after(expiresAt)) {
                throw Exception("OTP EXPIRED: Ask the customer to generate a new pickup OTP.")
            }

            if (storedOtp.trim() != enteredOtp.trim()) {
                throw Exception("INVALID OTP: Please check and ask the customer for the current pickup OTP.")
            }

            val now = Date()
            transaction.update(
                orderRef,
                mapOf(
                    "otpVerified" to true,
                    "otpVerifiedAt" to FieldValue.serverTimestamp(),
                    "status" to OrderStatus.PICKED_UP.name,
                    "updatedAt" to now
                )
            )
        }.await()
        Result.Success(Unit)
    } catch (e: Exception) {
        Result.Error(e.message ?: "Failed to verify pickup OTP", e)
    }

    /**
     * Atomically starts a trip for an assigned Order.
     * Uses VehicleAssignmentResolver for single-source-of-truth vehicle canonicalization.
     */
    override suspend fun startOrderTrip(orderId: String, driverId: String): Result<Unit> = try {
        requireAuth(driverId)
        val vehicleRepo = FirestoreVehicleRepository(firestore)
        val resolver = VehicleAssignmentResolver(vehicleRepo)

        // 1. Fetch order and driver records BEFORE transaction
        val orderSnapshot = firestore.collection("orders").document(orderId).get().await()
        if (!orderSnapshot.exists()) {
            throw Exception("TRIP NOT READY: Order record not found.")
        }

        val driverSnapshot = firestore.collection("drivers").document(driverId).get().await()
        if (!driverSnapshot.exists()) {
            throw Exception("TRIP NOT READY: Driver record not found.")
        }

        // Extract vehicle references independently
        val orderVehicleRef = orderSnapshot.getString("assignedVehicleId") 
            ?: orderSnapshot.getString("vehicleId")
            ?: orderSnapshot.getString("vehicleRegistration")
            ?: orderSnapshot.getString("vehicleNumber")

        val driverVehicleRef = driverSnapshot.getString("assignedVehicleId")
            ?: driverSnapshot.getString("assignedVehicle")
            ?: driverSnapshot.getString("vehicleRegistration")
            ?: driverSnapshot.getString("vehicleNumber")
            ?: driverSnapshot.getString("vehicleId")

        // 2. Resolve vehicle assignment using VehicleAssignmentResolver
        val resolution = resolver.resolve(
            tripVehicleRef = orderVehicleRef,
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
            val orderRef = firestore.collection("orders").document(orderId)
            val driverRef = firestore.collection("drivers").document(driverId)

            val orderDoc = transaction.get(orderRef)
            val driverDoc = transaction.get(driverRef)

            val vehicleRef = firestore.collection("vehicles").document(effectiveVehicleId)
            val vehicleDoc = transaction.get(vehicleRef)

            // ==========================================
            // 2. VALIDATIONS AFTER ALL READS
            // ==========================================
            if (!orderDoc.exists()) {
                throw Exception("TRIP NOT READY: Order record not found.")
            }
            if (!driverDoc.exists()) {
                throw Exception("TRIP NOT READY: Driver record not found.")
            }

            val authUid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
            val assignedDriver = orderDoc.getString("assignedDriverId") ?: orderDoc.getString("driverId")
            if (!assignedDriver.isNullOrBlank() && assignedDriver != driverId && assignedDriver != authUid) {
                throw Exception("TRIP ACCESS DENIED: Unauthorized driver.")
            }

            if (!vehicleDoc.exists()) {
                throw Exception("VEHICLE_RECORD_NOT_FOUND: Vehicle record ($effectiveVehicleId) not found in database.")
            }

            val vStatus = vehicleDoc.getString("status")?.uppercase()
            if (vStatus == "MAINTENANCE" || vStatus == "INACTIVE") {
                throw Exception("TRIP NOT READY: Assigned vehicle is currently $vStatus.")
            }

            val currentStatus = orderDoc.getString("status")?.uppercase() ?: ""
            if (currentStatus in listOf("DISPATCHED", "IN_TRANSIT", "DELIVERED", "COMPLETED")) {
                throw Exception("TRIP NOT READY: Trip is already active or delivered.")
            }

            val now = Date()

            // ==========================================
            // 3. ALL WRITES AFTER ALL READS
            // ==========================================
            // A. Update Order status
            transaction.update(
                orderRef,
                mapOf(
                    "status" to OrderStatus.DISPATCHED.name,
                    "assignedVehicleId" to effectiveVehicleId,
                    "updatedAt" to now
                )
            )

            // B. Update Driver Status to ON_DUTY
            transaction.update(
                driverRef,
                mapOf(
                    "status" to DriverStatus.ON_DUTY.name.lowercase(),
                    "lastActive" to now
                )
            )

            // C. Update Vehicle Status to IN_TRANSIT
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
        val msg = e.message ?: "Failed to start order trip"
        Result.Error(msg, e)
    }

    override suspend fun submitParcelDetails(
        orderId: String,
        driverId: String,
        parcelData: com.routecj.driver.domain.model.ParcelSubmissionData
    ): Result<Unit> = try {
        requireAuth(driverId)
        firestore.runTransaction { transaction ->
            val orderRef = firestore.collection("orders").document(orderId)
            val orderDoc = transaction.get(orderRef)

            if (!orderDoc.exists()) {
                throw Exception("Order #$orderId not found.")
            }

            val assignedDriver = orderDoc.getString("assignedDriverId") ?: orderDoc.getString("driverId")
            if (assignedDriver != driverId) {
                throw Exception("TRIP ACCESS DENIED: You are not authorized for this pickup.")
            }

            val driverArrived = orderDoc.getBoolean("driverArrived") ?: false
            if (!driverArrived) {
                throw Exception("Driver arrival confirmation required before submitting parcel details.")
            }

            val otpVerified = orderDoc.getBoolean("otpVerified") ?: false
            if (!otpVerified) {
                throw Exception("PICKUP VERIFICATION REQUIRED: Customer OTP must be verified before entering parcel details.")
            }

            val currentStatus = orderDoc.getString("status")?.uppercase() ?: "PENDING"
            if (currentStatus == OrderStatus.PENDING_GODOWN_REVIEW.name) {
                throw Exception("PARCEL ALREADY SUBMITTED: This parcel has already been submitted to the Godown Manager.")
            }

            if (currentStatus == OrderStatus.CANCELLED.name) {
                throw Exception("ORDER CANCELLED: This order has been cancelled.")
            }

            val now = Date()
            transaction.update(
                orderRef,
                mapOf(
                    "itemName" to parcelData.itemDescription.trim(),
                    "itemDescription" to parcelData.itemDescription.trim(),
                    "quantity" to parcelData.packageCount,
                    "weight" to parcelData.weight,
                    "specialInstructions" to parcelData.specialInstructions.trim(),
                    "status" to OrderStatus.PENDING_GODOWN_REVIEW.name,
                    "parcelSubmittedAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to now
                )
            )
        }.await()
        Result.Success(Unit)
    } catch (e: Exception) {
        Result.Error(e.message ?: "Failed to submit parcel details", e)
    }

    override suspend fun completeOrderTrip(orderId: String, driverId: String): Result<Unit> = try {
        requireAuth(driverId)
        val orderRef = firestore.collection("orders").document(orderId)
        
        val initialOrderDoc = orderRef.get().await()
        if (!initialOrderDoc.exists()) throw Exception("TRIP NOT FOUND: Order record not found.")

        val orderVehicleId = initialOrderDoc.getString("assignedVehicleId") 
            ?: initialOrderDoc.getString("vehicleId")
            ?: initialOrderDoc.getString("vehicleRegistration")
            
        val driverRef = firestore.collection("drivers").document(driverId)
        val driverLocationsRef = firestore.collection("driverLocations").document(driverId)

        val vehicleRepo = FirestoreVehicleRepository(firestore)
        val canonicalVehicleId = if (!orderVehicleId.isNullOrBlank()) {
            val res = vehicleRepo.getVehicleById(orderVehicleId)
            if (res is Result.Success) res.data.id else null
        } else null

        firestore.runTransaction { transaction ->
            val orderDoc = transaction.get(orderRef)
            
            if (orderDoc.getString("driverId") != driverId) {
                throw Exception("TRIP ACCESS DENIED: Unauthorized driver.")
            }

            val currentStatus = orderDoc.getString("status")?.uppercase()
            if (currentStatus == "DELIVERED" || currentStatus == "COMPLETED") {
                throw Exception("TRIP COMPLETED: Trip is already delivered.")
            }

            transaction.update(orderRef, mapOf(
                "status" to "DELIVERED",
                "deliveredAt" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
                "deliveredByUid" to driverId,
                "updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
            ))

            transaction.update(driverRef, mapOf(
                "status" to "AVAILABLE",
                "isLocationSharing" to false,
                "activeTripId" to null,
                "updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
            ))

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
