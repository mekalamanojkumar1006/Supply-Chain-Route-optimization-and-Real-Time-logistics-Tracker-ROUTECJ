package com.routecj.admin.data.repository

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.routecj.admin.core.util.Result
import com.routecj.admin.domain.model.*
import com.routecj.admin.domain.repository.DispatchRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.*
import javax.inject.Inject

class FirestoreDispatchRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) : DispatchRepository {

    private val dispatchesCollection = firestore.collection("dispatches")
    private val driversCollection = firestore.collection("drivers")
    private val vehiclesCollection = firestore.collection("vehicles")

    override suspend fun getAllDispatches(): Flow<Result<List<Dispatch>>> = callbackFlow {
        val listener = dispatchesCollection.addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(Result.Error(error.message ?: "Failed to listen to dispatches"))
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val dispatches = snapshot.documents.mapNotNull { doc ->
                    docToDispatch(doc.id, doc.data)
                }
                trySend(Result.Success(dispatches))
            }
        }
        awaitClose { listener.remove() }
    }

    override suspend fun getDispatchById(id: String): Result<Dispatch> = try {
        val doc = dispatchesCollection.document(id).get().await()
        if (doc.exists()) {
            Result.Success(docToDispatch(doc.id, doc.data)!!)
        } else {
            Result.Error("Dispatch not found")
        }
    } catch (e: Exception) {
        Result.Error(e.message ?: "Error getting dispatch")
    }

    override suspend fun createDispatch(dispatch: Dispatch): Result<Unit> = try {
        val data = dispatchToMap(dispatch)
        dispatchesCollection.document(dispatch.id.ifBlank { dispatchesCollection.document().id }).set(data).await()
        Result.Success(Unit)
    } catch (e: Exception) {
        Result.Error(e.message ?: "Error creating dispatch")
    }

    override suspend fun updateDispatch(dispatch: Dispatch): Result<Unit> = try {
        dispatchesCollection.document(dispatch.id).set(dispatchToMap(dispatch)).await()
        Result.Success(Unit)
    } catch (e: Exception) {
        Result.Error(e.message ?: "Error updating dispatch")
    }

    override suspend fun updateDispatchStatus(id: String, status: DispatchStatus): Result<Unit> = try {
        firestore.runTransaction { transaction ->
            val dispatchDoc = transaction.get(dispatchesCollection.document(id))
            val dispatch = docToDispatch(id, dispatchDoc.data) ?: throw Exception("Dispatch not found")
            
            // 1. Update Dispatch Status
            transaction.update(dispatchesCollection.document(id), "status", status.name, "updatedAt", Date())
            
            // 2. Handle Side Effects (Driver/Vehicle Availability)
            val driverId = dispatch.driverId
            val vehicleId = dispatch.vehicleId
            
            when (status) {
                DispatchStatus.TRIP_STARTED -> {
                    if (driverId != null) transaction.update(driversCollection.document(driverId), "status", DriverStatus.ON_DUTY.name)
                    if (vehicleId != null) transaction.update(vehiclesCollection.document(vehicleId), "status", VehicleStatus.IN_TRANSIT.name)
                }
                DispatchStatus.DELIVERED, DispatchStatus.CANCELLED -> {
                    if (driverId != null) transaction.update(driversCollection.document(driverId), "status", DriverStatus.AVAILABLE.name)
                    if (vehicleId != null) transaction.update(vehiclesCollection.document(vehicleId), "status", VehicleStatus.AVAILABLE.name)
                }
                else -> {}
            }
        }.await()
        Result.Success(Unit)
    } catch (e: Exception) {
        Result.Error(e.message ?: "Error updating status")
    }

    override suspend fun assignDriverAndVehicle(dispatchId: String, driverId: String, vehicleId: String): Result<Unit> = try {
        firestore.runTransaction { transaction ->
            val driverDoc = transaction.get(driversCollection.document(driverId))
            val vehicleDoc = transaction.get(vehiclesCollection.document(vehicleId))
            
            if (driverDoc.getString("status") != DriverStatus.AVAILABLE.name) throw Exception("Driver not available")
            if (vehicleDoc.getString("status") != VehicleStatus.AVAILABLE.name) throw Exception("Vehicle not available")
            
            // Fetch Driver Name and Vehicle Reg
            val driverName = driverDoc.getString("name") ?: "Unknown Driver"
            val vehicleReg = vehicleDoc.getString("vehicleNumber") ?: vehicleDoc.getString("registrationNumber") ?: "Unknown"

            // Update Dispatch
            transaction.update(dispatchesCollection.document(dispatchId), 
                "driverId", driverId,
                "driverName", driverName,
                "vehicleId", vehicleId,
                "vehicleRegistration", vehicleReg,
                "status", DispatchStatus.ASSIGNED.name,
                "updatedAt", Date()
            )
            
            // Update Driver/Vehicle to ASSIGNED (not yet IN_USE)
            transaction.update(driversCollection.document(driverId), "status", DriverStatus.BUSY.name)
            transaction.update(vehiclesCollection.document(vehicleId), "status", VehicleStatus.ASSIGNED.name)
        }.await()
        Result.Success(Unit)
    } catch (e: Exception) {
        Result.Error(e.message ?: "Error assigning")
    }

    override suspend fun createDispatchFromOrder(order: Order, driverId: String, vehicleId: String): Result<Unit> = try {
        firestore.runTransaction { transaction ->
            val orderRef = firestore.collection("orders").document(order.id)
            val orderDoc = transaction.get(orderRef)
            
            val currentStatus = orderDoc.getString("status")?.uppercase() ?: ""
            if (currentStatus == OrderStatus.DISPATCHED.name || currentStatus == "IN_TRANSIT" || currentStatus == "DELIVERED") {
                throw Exception("This parcel has already been dispatched.")
            }

            val driverDoc = transaction.get(driversCollection.document(driverId))
            val vehicleDoc = transaction.get(vehiclesCollection.document(vehicleId))
            
            val driverName = driverDoc.getString("name") ?: "Unknown Driver"
            val driverPhone = driverDoc.getString("phone") ?: ""
            val vehicleReg = vehicleDoc.getString("registrationNumber") ?: vehicleDoc.getString("vehicleNumber") ?: "Unknown"
            val vehicleType = vehicleDoc.getString("vehicleType") ?: "VAN"

            // 1. Create Dispatch
            val dispatchId = dispatchesCollection.document().id
            val dispatch = Dispatch(
                id = dispatchId,
                orderId = order.id,
                orderNumber = order.orderNumber,
                customerName = order.customerName,
                pickupLocation = order.pickupAddress.ifBlank { order.pickupLocation },
                deliveryLocation = order.deliveryAddress.ifBlank { order.deliveryLocation },
                driverId = driverId,
                driverName = driverName,
                vehicleId = vehicleId,
                vehicleRegistration = vehicleReg,
                status = DispatchStatus.DISPATCH_CONFIRMED,
                priority = order.priority
            )
            transaction.set(dispatchesCollection.document(dispatchId), dispatchToMap(dispatch))

            // 2. Update Order
            transaction.update(orderRef, 
                "status", OrderStatus.DISPATCHED.name,
                "assignedDriverId", driverId,
                "driverId", driverId,
                "driverName", driverName,
                "driverPhone", driverPhone,
                "assignedVehicleId", vehicleId,
                "vehicleId", vehicleId,
                "vehicleRegistration", vehicleReg,
                "vehicleType", vehicleType,
                "updatedAt", Date()
            )

            // 3. Update Driver/Vehicle status
            transaction.update(driversCollection.document(driverId), "status", DriverStatus.BUSY.name)
            transaction.update(vehiclesCollection.document(vehicleId), "status", VehicleStatus.ASSIGNED.name)
        }.await()
        Result.Success(Unit)
    } catch (e: Exception) {
        Result.Error(e.message ?: "Failed to create dispatch")
    }

    private fun docToDispatch(id: String, data: Map<String, Any>?): Dispatch? {
        if (data == null) return null
        return Dispatch(
            id = id,
            orderId = data["orderId"] as? String ?: "",
            orderNumber = data["orderNumber"] as? String ?: "",
            customerName = data["customerName"] as? String ?: "",
            pickupLocation = data["pickupLocation"] as? String ?: "",
            deliveryLocation = data["deliveryLocation"] as? String ?: "",
            driverId = data["driverId"] as? String,
            driverName = data["driverName"] as? String,
            vehicleId = data["vehicleId"] as? String,
            vehicleRegistration = data["vehicleRegistration"] as? String,
            status = try { DispatchStatus.valueOf(data["status"] as String) } catch (_: Exception) { DispatchStatus.PENDING },
            priority = data["priority"] as? String ?: "Medium",
            estimatedDelivery = (data["estimatedDelivery"] as? com.google.firebase.Timestamp)?.toDate(),
            createdAt = (data["createdAt"] as? com.google.firebase.Timestamp)?.toDate() ?: Date(),
            updatedAt = (data["updatedAt"] as? com.google.firebase.Timestamp)?.toDate() ?: Date(),
            remarks = data["remarks"] as? String
        )
    }

    private fun dispatchToMap(dispatch: Dispatch): Map<String, Any?> {
        return mapOf(
            "orderId" to dispatch.orderId,
            "orderNumber" to dispatch.orderNumber,
            "customerName" to dispatch.customerName,
            "pickupLocation" to dispatch.pickupLocation,
            "deliveryLocation" to dispatch.deliveryLocation,
            "driverId" to dispatch.driverId,
            "driverName" to dispatch.driverName,
            "vehicleId" to dispatch.vehicleId,
            "vehicleRegistration" to dispatch.vehicleRegistration,
            "status" to dispatch.status.name,
            "priority" to dispatch.priority,
            "estimatedDelivery" to dispatch.estimatedDelivery,
            "createdAt" to dispatch.createdAt,
            "updatedAt" to dispatch.updatedAt,
            "remarks" to dispatch.remarks
        )
    }
}
