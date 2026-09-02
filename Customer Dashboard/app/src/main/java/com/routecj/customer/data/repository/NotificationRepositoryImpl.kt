package com.routecj.customer.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.routecj.customer.domain.model.CustomerNotification
import com.routecj.customer.domain.repository.NotificationRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class NotificationRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : NotificationRepository {

    private val notifications = firestore.collection("notifications")

    override fun getNotificationsFlow(customerId: String): Flow<Result<List<CustomerNotification>>> =
        callbackFlow {
            val listener = notifications
                .whereEqualTo("customerId", customerId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(50)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        trySend(Result.failure(error))
                        return@addSnapshotListener
                    }
                    val items = snapshot?.documents?.mapNotNull { doc ->
                        try {
                            CustomerNotification(
                                notificationId = doc.id,
                                customerId = doc.getString("customerId") ?: customerId,
                                orderId = doc.getString("orderId"),
                                title = doc.getString("title") ?: "",
                                message = doc.getString("message") ?: "",
                                type = doc.getString("type") ?: "GENERAL",
                                createdAt = doc.getLong("createdAt") ?: 0L,
                                read = doc.getBoolean("read") ?: false
                            )
                        } catch (e: Exception) { null }
                    } ?: emptyList()
                    trySend(Result.success(items))
                }
            awaitClose { listener.remove() }
        }

    override fun getUnreadCountFlow(customerId: String): Flow<Int> = callbackFlow {
        val listener = notifications
            .whereEqualTo("customerId", customerId)
            .whereEqualTo("read", false)
            .addSnapshotListener { snapshot, _ ->
                trySend(snapshot?.size() ?: 0)
            }
        awaitClose { listener.remove() }
    }

    override suspend fun markAsRead(notificationId: String, customerId: String): Result<Unit> {
        return try {
            val doc = notifications.document(notificationId).get().await()
            // Only update if it belongs to this customer (defence in depth)
            if (doc.getString("customerId") == customerId) {
                notifications.document(notificationId)
                    .update("read", true)
                    .await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun markAllAsRead(customerId: String): Result<Unit> {
        return try {
            val unread = notifications
                .whereEqualTo("customerId", customerId)
                .whereEqualTo("read", false)
                .get()
                .await()

            if (unread.isEmpty) return Result.success(Unit)

            val batch = firestore.batch()
            unread.documents.forEach { doc ->
                batch.update(doc.reference, "read", true)
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun saveNotification(notification: CustomerNotification): Result<Unit> {
        return try {
            // Use stable notificationId for deduplication — prevents same event creating duplicates
            val docId = notification.notificationId.ifBlank {
                "${notification.type}_${notification.orderId ?: "general"}_${notification.customerId.take(8)}"
            }
            // setMerge = false → only write if it does not already exist (idempotent create)
            val docRef = notifications.document(docId)
            val existing = docRef.get().await()
            if (!existing.exists()) {
                docRef.set(
                    mapOf(
                        "notificationId" to docId,
                        "customerId" to notification.customerId,
                        "orderId" to notification.orderId,
                        "title" to notification.title,
                        "message" to notification.message,
                        "type" to notification.type,
                        "createdAt" to notification.createdAt,
                        "read" to false
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
