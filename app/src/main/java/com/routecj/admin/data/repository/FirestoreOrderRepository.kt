package com.routecj.admin.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.routecj.admin.core.util.OrderAddressMapper
import com.routecj.admin.core.util.Result
import com.routecj.admin.domain.model.Location
import com.routecj.admin.domain.model.Order
import com.routecj.admin.domain.model.OrderItem
import com.routecj.admin.domain.model.OrderStatus
import com.routecj.admin.domain.repository.OrderRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.Date
import javax.inject.Inject

/**
 * Firestore-based implementation of OrderRepository.
 * Provides real-time synchronization with the 'orders' collection.
 */
class FirestoreOrderRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) : OrderRepository {

    private val collection = firestore.collection("orders")

    override suspend fun getAllOrders(): Flow<Result<List<Order>>> = callbackFlow {
        trySend(Result.Loading())

        val listener: ListenerRegistration = collection.addSnapshotListener { snapshots, error ->
            if (error != null) {
                trySend(Result.Error("Firestore error: ${error.message}"))
                return@addSnapshotListener
            }

            if (snapshots == null) {
                trySend(Result.Error("No data available"))
                return@addSnapshotListener
            }

            val orders = snapshots.documents.mapNotNull { doc ->
                try {
                    docToOrder(doc.id, doc.data)
                } catch (t: Throwable) {
                    null
                }
            }

            trySend(Result.Success(orders))
        }

        awaitClose { listener.remove() }
    }

    override suspend fun getOrderById(orderId: String): Result<Order> {
        return try {
            val doc = collection.document(orderId).get().await()
            if (doc.exists()) {
                Result.Success(docToOrder(doc.id, doc.data))
            } else {
                // Try searching by verificationToken, parcelId, or orderNumber
                val tokenQuery = collection.whereEqualTo("verificationToken", orderId).limit(1).get().await()
                if (!tokenQuery.isEmpty) {
                    val matchDoc = tokenQuery.documents.first()
                    return Result.Success(docToOrder(matchDoc.id, matchDoc.data))
                }
                
                val parcelIdQuery = collection.whereEqualTo("parcelId", orderId).limit(1).get().await()
                if (!parcelIdQuery.isEmpty) {
                    val matchDoc = parcelIdQuery.documents.first()
                    return Result.Success(docToOrder(matchDoc.id, matchDoc.data))
                }

                val orderNumQuery = collection.whereEqualTo("orderNumber", orderId).limit(1).get().await()
                if (!orderNumQuery.isEmpty) {
                    val matchDoc = orderNumQuery.documents.first()
                    return Result.Success(docToOrder(matchDoc.id, matchDoc.data))
                }

                Result.Error("Order/Parcel not found for ID: $orderId")
            }
        } catch (e: Exception) {
            Result.Error("Error fetching order: ${e.message}", throwable = e)
        }
    }

    override suspend fun createOrder(order: Order): Result<Order> {
        return try {
            val data = orderToMap(order)
            val docId = if (order.id.isNotBlank()) order.id else collection.document().id
            val finalData = data.toMutableMap().apply { put("id", docId) }
            collection.document(docId).set(finalData).await()
            val created = collection.document(docId).get().await()
            Result.Success(docToOrder(created.id, created.data))
        } catch (e: Exception) {
            Result.Error("Error creating order: ${e.message}", throwable = e)
        }
    }

    override suspend fun updateOrder(order: Order): Result<Order> {
        return try {
            val data = orderToMap(order)
            collection.document(order.id).set(data).await()
            val updated = collection.document(order.id).get().await()
            Result.Success(docToOrder(updated.id, updated.data))
        } catch (e: Exception) {
            Result.Error("Error updating order: ${e.message}", throwable = e)
        }
    }

    override suspend fun deleteOrder(orderId: String): Result<Boolean> {
        return try {
            collection.document(orderId).delete().await()
            Result.Success(true)
        } catch (e: Exception) {
            Result.Error("Error deleting order: ${e.message}", throwable = e)
        }
    }

    override suspend fun getOrdersByStatus(status: String): Result<List<Order>> {
        return try {
            val querySnapshot = collection.whereEqualTo("status", status.lowercase()).get().await()
            val orders = querySnapshot.documents.mapNotNull { doc ->
                try { docToOrder(doc.id, doc.data) } catch (t: Throwable) { null }
            }
            Result.Success(orders)
        } catch (e: Exception) {
            Result.Error("Error querying orders: ${e.message}", throwable = e)
        }
    }

    // --- Helpers ---
    private fun docToOrder(id: String, data: Map<String, Any>?): Order {
        val map = data ?: emptyMap()

        val (canonicalPickupAddress, canonicalPickupPincode, originLoc) = OrderAddressMapper.extractPickupInfo(map)
        val (canonicalDeliveryAddress, canonicalDeliveryPincode, destLoc) = OrderAddressMapper.extractDeliveryInfo(map)

        val orderNumber = map["orderNumber"] as? String ?: "#${id.take(6)}"
        val customerName = map["customerName"] as? String ?: ""
        val customerPhone = map["customerPhone"] as? String ?: ""
        val customerAddress = (map["customerAddress"] as? String)?.ifBlank { null } ?: canonicalDeliveryAddress
        val pickupLocation = canonicalPickupAddress
        val deliveryLocation = canonicalDeliveryAddress
        val pickupAddress = canonicalPickupAddress
        val pickupPincode = canonicalPickupPincode
        val deliveryAddress = canonicalDeliveryAddress
        val deliveryPincode = canonicalDeliveryPincode
        val orderType = map["orderType"] as? String ?: ""
        val weight = (map["weight"] as? Number)?.toDouble() ?: 0.0
        val quantity = (map["quantity"] as? Number)?.toInt() ?: 0
        val priority = map["priority"] as? String ?: "Medium"
        val paymentStatus = map["paymentStatus"] as? String ?: "Pending"
        
        val statusStr = (map["status"] as? String) ?: "PENDING"
        val status = try { OrderStatus.valueOf(statusStr.uppercase()) } catch (_: Exception) { OrderStatus.PENDING }

        val assignedDriverId = map["assignedDriverId"] as? String
        val assignedVehicleId = map["assignedVehicleId"] as? String
        
        val estimatedDeliveryDate = (map["estimatedDeliveryDate"] as? com.google.firebase.Timestamp)?.toDate()
            ?: (map["estimatedDeliveryDate"] as? Date)
            
        val remarks = map["remarks"] as? String ?: ""

        val createdAt = (map["createdAt"] as? com.google.firebase.Timestamp)?.toDate()
            ?: (map["createdAt"] as? Date) ?: Date()
        val updatedAt = (map["updatedAt"] as? com.google.firebase.Timestamp)?.toDate()
            ?: (map["updatedAt"] as? Date) ?: Date()

        val origin = if (originLoc.address.isNotBlank()) originLoc else Location(
            latitude = 0.0,
            longitude = 0.0,
            address = pickupAddress,
            city = "",
            state = "",
            pincode = pickupPincode
        )

        val destination = if (destLoc.address.isNotBlank()) destLoc else Location(
            latitude = 0.0,
            longitude = 0.0,
            address = deliveryAddress,
            city = "",
            state = "",
            pincode = deliveryPincode
        )

        return Order(
            id = id,
            orderNumber = orderNumber,
            customerName = customerName,
            customerPhone = customerPhone,
            customerAddress = customerAddress,
            pickupLocation = pickupLocation.ifBlank { pickupAddress },
            deliveryLocation = deliveryLocation.ifBlank { deliveryAddress },
            pickupAddress = pickupAddress,
            pickupPincode = pickupPincode,
            deliveryAddress = deliveryAddress,
            deliveryPincode = deliveryPincode,
            orderType = orderType,
            weight = weight,
            quantity = quantity,
            priority = priority,
            paymentStatus = paymentStatus,
            status = status,
            assignedDriverId = assignedDriverId,
            assignedVehicleId = assignedVehicleId,
            estimatedDeliveryDate = estimatedDeliveryDate,
            remarks = remarks,
            createdAt = createdAt,
            updatedAt = updatedAt,
            
            // Internal compatibility
            customerId = map["customerId"] as? String ?: "",
            driverId = (map["driverId"] as? String) ?: assignedDriverId,
            driverName = map["driverName"] as? String,
            driverPhone = map["driverPhone"] as? String,
            vehicleId = (map["vehicleId"] as? String) ?: assignedVehicleId,
            vehicleRegistration = map["vehicleRegistration"] as? String,
            vehicleType = map["vehicleType"] as? String,
            godownId = map["godownId"] as? String,
            origin = origin,
            destination = destination,
            totalAmount = (map["totalAmount"] as? Number)?.toDouble() ?: 0.0,
            trackingId = map["trackingId"] as? String ?: "",
            estimatedTime = map["estimatedTime"] as? String ?: "",
            notes = map["notes"] as? String ?: "",

            // Role-specific workflow fields
            otpVerified = map["otpVerified"] as? Boolean ?: false,
            qrId = map["qrId"] as? String,
            qrStatus = map["qrStatus"] as? String,
            qrGeneratedBy = map["qrGeneratedBy"] as? String,
            qrGeneratedAt = (map["qrGeneratedAt"] as? com.google.firebase.Timestamp)?.toDate(),
            verificationToken = map["verificationToken"] as? String,
            parcelId = map["parcelId"] as? String,

            // Godown parcel and item specifications
            itemName = map["itemName"] as? String ?: "",
            itemDescription = map["itemDescription"] as? String ?: "",
            length = (map["length"] as? Number)?.toDouble() ?: 0.0,
            width = (map["width"] as? Number)?.toDouble() ?: 0.0,
            height = (map["height"] as? Number)?.toDouble() ?: 0.0,
            isFragile = map["isFragile"] as? Boolean ?: false,
            specialInstructions = map["specialInstructions"] as? String ?: "",

            // Audit and creator fields
            createdBy = map["createdBy"] as? String ?: "",
            createdByUid = map["createdByUid"] as? String ?: "",
            createdByRole = map["createdByRole"] as? String ?: "",
            source = map["source"] as? String ?: "APP",

            // Delivery completion metadata
            deliveredAt = (map["deliveredAt"] as? com.google.firebase.Timestamp)?.toDate()
                ?: (map["deliveredAt"] as? Date),
            deliveredBy = map["deliveredBy"] as? String ?: "",
            deliveredByUid = map["deliveredByUid"] as? String ?: "",
            deliveryOtp = map["deliveryOtp"] as? String ?: "",
            deliveryRemarks = map["deliveryRemarks"] as? String ?: ""
        )
    }

    override suspend fun completeDeliveryAtomic(
        orderId: String,
        dispatchId: String?,
        deliveryOtp: String?,
        remarks: String?,
        deliveredBy: String,
        deliveredByUid: String
    ): Result<Unit> {
        return try {
            firestore.runTransaction { transaction ->
                val orderRef = collection.document(orderId)
                val orderDoc = transaction.get(orderRef)

                if (!orderDoc.exists()) {
                    throw Exception("Order #$orderId not found.")
                }

                val currentStatus = orderDoc.getString("status")?.uppercase() ?: ""
                if (currentStatus == OrderStatus.DELIVERED.name) {
                    throw Exception("Delivery already completed.")
                }

                val now = Date()

                // 1. Update Order document atomically
                val orderUpdates = mutableMapOf<String, Any>(
                    "status" to OrderStatus.DELIVERED.name,
                    "deliveredAt" to now,
                    "deliveredBy" to deliveredBy,
                    "deliveredByUid" to deliveredByUid,
                    "updatedAt" to now
                )
                if (!deliveryOtp.isNullOrBlank()) {
                    orderUpdates["deliveryOtp"] = deliveryOtp
                }
                if (!remarks.isNullOrBlank()) {
                    orderUpdates["deliveryRemarks"] = remarks
                }
                transaction.update(orderRef, orderUpdates)

                // 2. Update Dispatch document if linked
                val dId = dispatchId ?: orderDoc.getString("dispatchId")
                var driverId = orderDoc.getString("assignedDriverId")
                var vehicleId = orderDoc.getString("assignedVehicleId")

                if (!dId.isNullOrBlank()) {
                    val dispatchRef = firestore.collection("dispatches").document(dId)
                    val dispatchDoc = transaction.get(dispatchRef)
                    if (dispatchDoc.exists()) {
                        transaction.update(
                            dispatchRef,
                            "status", "DELIVERED",
                            "updatedAt", now,
                            "remarks", remarks ?: (dispatchDoc.getString("remarks") ?: "")
                        )
                        if (driverId.isNullOrBlank()) {
                            driverId = dispatchDoc.getString("driverId")
                        }
                        if (vehicleId.isNullOrBlank()) {
                            vehicleId = dispatchDoc.getString("vehicleId")
                        }
                    }
                }

                // 3. Free up Driver (Status: AVAILABLE)
                if (!driverId.isNullOrBlank()) {
                    val driverRef = firestore.collection("drivers").document(driverId)
                    val driverDoc = transaction.get(driverRef)
                    if (driverDoc.exists()) {
                        transaction.update(driverRef, "status", "AVAILABLE", "lastActive", now)
                    }
                }

                // 4. Free up Vehicle (Status: AVAILABLE)
                if (!vehicleId.isNullOrBlank()) {
                    val vehicleRef = firestore.collection("vehicles").document(vehicleId)
                    val vehicleDoc = transaction.get(vehicleRef)
                    if (vehicleDoc.exists()) {
                        transaction.update(vehicleRef, "status", "AVAILABLE", "updatedAt", now)
                    }
                }
            }.await()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to complete delivery atomic transaction", throwable = e)
        }
    }

    private fun orderToMap(order: Order): Map<String, Any> {
        val pickupAddr = order.pickupAddress.ifBlank { order.pickupLocation }
        val pickupPin = order.pickupPincode.ifBlank { OrderAddressMapper.extractPincodeFromText(pickupAddr) }
        val delivAddr = order.deliveryAddress.ifBlank { order.deliveryLocation.ifBlank { order.customerAddress } }
        val delivPin = order.deliveryPincode.ifBlank { OrderAddressMapper.extractPincodeFromText(delivAddr) }

        val map = mutableMapOf<String, Any>(
            "orderNumber" to order.orderNumber,
            "customerName" to order.customerName,
            "customerPhone" to order.customerPhone,
            "customerAddress" to delivAddr,
            "pickupLocation" to pickupAddr,
            "deliveryLocation" to delivAddr,
            "pickupAddress" to pickupAddr,
            "pickup_address" to pickupAddr,
            "pickupPincode" to pickupPin,
            "pickup_pincode" to pickupPin,
            "deliveryAddress" to delivAddr,
            "delivery_address" to delivAddr,
            "deliveryPincode" to delivPin,
            "delivery_pincode" to delivPin,
            "destinationAddress" to delivAddr,
            "destinationPincode" to delivPin,
            "originAddress" to pickupAddr,
            "originPincode" to pickupPin,
            "dropAddress" to delivAddr,
            "dropPincode" to delivPin,
            "receiverAddress" to delivAddr,
            "receiverPincode" to delivPin,
            "orderType" to order.orderType,
            "weight" to order.weight,
            "quantity" to order.quantity,
            "priority" to order.priority,
            "paymentStatus" to order.paymentStatus,
            "status" to order.status.name,
            "assignedDriverId" to (order.assignedDriverId ?: ""),
            "assignedVehicleId" to (order.assignedVehicleId ?: ""),
            "estimatedDeliveryDate" to (order.estimatedDeliveryDate ?: ""),
            "remarks" to order.remarks,
            "createdAt" to order.createdAt,
            "updatedAt" to order.updatedAt,
            
            // Compatibility
            "customerId" to order.customerId,
            "driverId" to (order.driverId ?: ""),
            "vehicleId" to (order.vehicleId ?: ""),
            "godownId" to (order.godownId ?: ""),
            "origin" to mapOf(
                "latitude" to order.origin.latitude,
                "longitude" to order.origin.longitude,
                "address" to pickupAddr,
                "city" to order.origin.city,
                "state" to order.origin.state,
                "pincode" to pickupPin
            ),
            "destination" to mapOf(
                "latitude" to order.destination.latitude,
                "longitude" to order.destination.longitude,
                "address" to delivAddr,
                "city" to order.destination.city,
                "state" to order.destination.state,
                "pincode" to delivPin
            ),
            "totalAmount" to order.totalAmount,
            "trackingId" to order.trackingId,
            "estimatedTime" to order.estimatedTime,
            "notes" to order.notes,

            // Role-specific workflow fields
            "otpVerified" to order.otpVerified,

            // Godown parcel and item specifications
            "itemName" to order.itemName,
            "itemDescription" to order.itemDescription,
            "length" to order.length,
            "width" to order.width,
            "height" to order.height,
            "isFragile" to order.isFragile,
            "specialInstructions" to order.specialInstructions,

            // Audit and creator fields
            "createdBy" to order.createdBy,
            "createdByUid" to order.createdByUid,
            "createdByRole" to order.createdByRole,
            "source" to order.source,

            // Delivery completion metadata
            "deliveredBy" to order.deliveredBy,
            "deliveredByUid" to order.deliveredByUid,
            "deliveryOtp" to order.deliveryOtp,
            "deliveryRemarks" to order.deliveryRemarks
        )

        order.deliveredAt?.let { map["deliveredAt"] = it }
        order.qrId?.let { map["qrId"] = it }
        order.qrStatus?.let { map["qrStatus"] = it }
        order.qrGeneratedBy?.let { map["qrGeneratedBy"] = it }
        order.qrGeneratedAt?.let { map["qrGeneratedAt"] = it }
        order.verificationToken?.let { map["verificationToken"] = it }
        order.parcelId?.let { map["parcelId"] = it }

        return map
    }
}

