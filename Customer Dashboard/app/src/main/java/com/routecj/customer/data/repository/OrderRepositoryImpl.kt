package com.routecj.customer.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.routecj.customer.core.error.toDataError
import com.routecj.customer.domain.model.Order
import com.routecj.customer.domain.repository.OrderRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class OrderRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : OrderRepository {

    private val collection = firestore.collection("orders")

    override suspend fun getOrder(orderId: String): Result<Order> {
        return try {
            val snapshot = collection.document(orderId).get().await()
            if (snapshot.exists()) {
                val order = mapDocumentToOrder(snapshot)
                Result.success(order)
            } else {
                Result.failure(Exception("Order not found").toDataError())
            }
        } catch (e: Exception) {
            Result.failure(e.toDataError())
        }
    }

    override suspend fun getOrdersByCustomer(customerId: String): Result<List<Order>> {
        return try {
            val snapshot = collection.whereEqualTo("customerId", customerId).get().await()
            val orders = snapshot.documents.map { mapDocumentToOrder(it) }
            Result.success(orders)
        } catch (e: Exception) {
            Result.failure(e.toDataError())
        }
    }

    override suspend fun createOrder(order: Order): Result<Unit> {
        return try {
            val data = hashMapOf(
                "customerId" to order.customerId,
                "pickupLatitude" to order.pickupLatitude,
                "pickupLongitude" to order.pickupLongitude,
                "pickupAddress" to order.pickupAddress,
                "destinationLatitude" to order.destinationLatitude,
                "destinationLongitude" to order.destinationLongitude,
                "destinationAddress" to order.destinationAddress,
                "packageType" to order.packageType,
                "itemDescription" to order.itemDescription,
                "packageCount" to order.packageCount,
                "weight" to order.weight,
                "specialInstructions" to order.specialInstructions,
                "pickupDate" to order.pickupDate,
                "pickupSlot" to order.pickupSlot,
                "status" to order.status.name,
                "createdAt" to order.createdAt,
                "updatedAt" to order.updatedAt
            )
            collection.document(order.id).set(data).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e.toDataError())
        }
    }

    private fun mapDocumentToOrder(doc: com.google.firebase.firestore.DocumentSnapshot): Order {
        val statusString = doc.getString("status") ?: "BOOKED"
        val status = try { com.routecj.customer.domain.model.OrderStatus.valueOf(statusString) } catch (e: Exception) { com.routecj.customer.domain.model.OrderStatus.BOOKED }
        
        return Order(
            id = doc.id,
            customerId = doc.getString("customerId") ?: "",
            pickupLatitude = doc.getDouble("pickupLatitude") ?: 0.0,
            pickupLongitude = doc.getDouble("pickupLongitude") ?: 0.0,
            pickupAddress = doc.getString("pickupAddress"),
            destinationLatitude = doc.getDouble("destinationLatitude"),
            destinationLongitude = doc.getDouble("destinationLongitude"),
            destinationAddress = doc.getString("destinationAddress"),
            packageType = doc.getString("packageType"),
            itemDescription = doc.getString("itemDescription"),
            packageCount = doc.getLong("packageCount")?.toInt(),
            weight = doc.getDouble("weight"),
            specialInstructions = doc.getString("specialInstructions"),
            pickupDate = doc.getString("pickupDate"),
            pickupSlot = doc.getString("pickupSlot"),
            status = status,
            driverId = doc.getString("driverId"),
            pickupOtpStatus = doc.getString("pickupOtpStatus"),
            pickupOtpCreatedAt = doc.getLong("pickupOtpCreatedAt"),
            pickupOtpExpiresAt = doc.getLong("pickupOtpExpiresAt"),
            parcelSubmitted = doc.getBoolean("parcelSubmitted"),
            parcelSubmittedAt = doc.getLong("parcelSubmittedAt"),
            qrGenerated = doc.getBoolean("qrGenerated"),
            qrCode = doc.getString("qrCode"),
            qrStatus = doc.getString("qrStatus"),
            paymentStatus = doc.getString("paymentStatus"),
            transactionId = doc.getString("transactionId"),
            paidAt = doc.getLong("paidAt"),
            deliveryCharge = doc.getDouble("deliveryCharge"),
            tax = doc.getDouble("tax"),
            totalAmount = doc.getDouble("totalAmount"),
            invoiceNumber = doc.getString("invoiceNumber"),
            paymentMode = doc.getString("paymentMode"),
            currency = doc.getString("currency"),
            createdAt = doc.getLong("createdAt") ?: 0L,
            updatedAt = doc.getLong("updatedAt") ?: 0L
        )
    }

    override fun getOrdersFlowByCustomer(customerId: String): kotlinx.coroutines.flow.Flow<Result<List<Order>>> = kotlinx.coroutines.flow.callbackFlow {
        val listenerRegistration = collection
            .whereEqualTo("customerId", customerId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Result.failure(error.toDataError()))
                    return@addSnapshotListener
                }
                
                if (snapshot != null) {
                    val orders = snapshot.documents.map { mapDocumentToOrder(it) }
                    // Sort descending by createdAt to show newest first
                    val sortedOrders = orders.sortedByDescending { it.createdAt }
                    trySend(Result.success(sortedOrders))
                }
            }
            
        awaitClose {
            listenerRegistration.remove()
        }
    }

    override fun getOrderFlow(orderId: String): kotlinx.coroutines.flow.Flow<Result<Order>> = kotlinx.coroutines.flow.callbackFlow {
        val listenerRegistration = collection.document(orderId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Result.failure(error.toDataError()))
                    return@addSnapshotListener
                }
                
                if (snapshot != null && snapshot.exists()) {
                    val order = mapDocumentToOrder(snapshot)
                    trySend(Result.success(order))
                } else {
                    trySend(Result.failure(Exception("Order not found").toDataError()))
                }
            }
            
        awaitClose {
            listenerRegistration.remove()
        }
    }
}
