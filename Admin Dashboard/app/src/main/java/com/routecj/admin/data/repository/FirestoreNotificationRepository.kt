package com.routecj.admin.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.routecj.admin.core.util.Result
import com.routecj.admin.domain.model.AdminRole
import com.routecj.admin.domain.model.Notification
import com.routecj.admin.domain.model.NotificationPriority
import com.routecj.admin.domain.model.NotificationType
import com.routecj.admin.domain.repository.NotificationRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.*
import javax.inject.Inject

class FirestoreNotificationRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) : NotificationRepository {

    private val collection = firestore.collection("notifications")

    override suspend fun getNotifications(role: AdminRole, uid: String): Flow<Result<List<Notification>>> = callbackFlow {
        val roleNotifications = mutableMapOf<String, Notification>()
        val uidNotifications = mutableMapOf<String, Notification>()

        fun emitMerged() {
            val merged = (roleNotifications.values + uidNotifications.values)
                .distinctBy { it.id }
                .sortedByDescending { it.createdAt }
            trySend(Result.Success(merged))
        }

        val listeners = mutableListOf<ListenerRegistration>()

        if (role == AdminRole.SUPER_ADMIN) {
            listeners.add(
                collection.orderBy("createdAt", Query.Direction.DESCENDING)
                    .addSnapshotListener { snapshot, error ->
                        if (error != null) {
                            trySend(Result.Error("Notification sync failed: ${error.message}"))
                            return@addSnapshotListener
                        }
                        if (snapshot != null) {
                            val notifications = snapshot.documents.mapNotNull { doc ->
                                docToNotification(doc.id, doc.data)
                            }
                            trySend(Result.Success(notifications))
                        }
                    }
            )
        } else {
            // 1. Role-based & Global
            listeners.add(
                collection.whereIn("recipientRole", listOf(role.name, "GLOBAL"))
                    .addSnapshotListener { snapshot, error ->
                        if (error != null) {
                            android.util.Log.e("AUTH_FIRESTORE", "Role notifications FAILED: ${error.message}")
                            return@addSnapshotListener
                        }
                        if (snapshot != null) {
                            snapshot.documents.forEach { doc ->
                                docToNotification(doc.id, doc.data)?.let { roleNotifications[it.id] = it }
                            }
                            emitMerged()
                        }
                    }
            )

            // 2. UID-specific
            listeners.add(
                collection.whereEqualTo("recipientId", uid)
                    .addSnapshotListener { snapshot, error ->
                        if (error != null) {
                            android.util.Log.e("AUTH_FIRESTORE", "UID notifications FAILED: ${error.message}")
                            return@addSnapshotListener
                        }
                        if (snapshot != null) {
                            snapshot.documents.forEach { doc ->
                                docToNotification(doc.id, doc.data)?.let { uidNotifications[it.id] = it }
                            }
                            emitMerged()
                        }
                    }
            )
        }

        awaitClose {
            listeners.forEach { it.remove() }
        }
    }

    override suspend fun markAsRead(notificationId: String): Result<Unit> = try {
        collection.document(notificationId).update("isRead", true, "updatedAt", Date()).await()
        Result.Success(Unit)
    } catch (e: Exception) {
        Result.Error(e.message ?: "Error marking notification as read")
    }

    override suspend fun markAllAsRead(role: AdminRole, uid: String): Result<Unit> = try {
        val batch = firestore.batch()
        
        if (role == AdminRole.SUPER_ADMIN) {
            val unread = collection.whereEqualTo("isRead", false).get().await()
            unread.documents.forEach { doc ->
                batch.update(doc.reference, "isRead", true, "updatedAt", Date())
            }
        } else {
            // 1. Mark Role-based & Global as read
            val roleUnread = collection.whereEqualTo("isRead", false)
                .whereIn("recipientRole", listOf(role.name, "GLOBAL"))
                .get().await()
            roleUnread.documents.forEach { doc ->
                batch.update(doc.reference, "isRead", true, "updatedAt", Date())
            }

            // 2. Mark UID-specific as read
            val uidUnread = collection.whereEqualTo("isRead", false)
                .whereEqualTo("recipientId", uid)
                .get().await()
            uidUnread.documents.forEach { doc ->
                batch.update(doc.reference, "isRead", true, "updatedAt", Date())
            }
        }

        batch.commit().await()
        Result.Success(Unit)
    } catch (e: Exception) {
        Result.Error(e.message ?: "Error marking all as read")
    }

    override suspend fun deleteNotification(notificationId: String): Result<Unit> = try {
        collection.document(notificationId).delete().await()
        Result.Success(Unit)
    } catch (e: Exception) {
        Result.Error(e.message ?: "Error deleting notification")
    }

    override suspend fun createNotification(notification: Notification): Result<Unit> = try {
        val docRef = collection.document()
        val data = notificationToMap(notification.copy(id = docRef.id))
        docRef.set(data).await()
        Result.Success(Unit)
    } catch (e: Exception) {
        Result.Error(e.message ?: "Error creating notification")
    }

    private fun docToNotification(id: String, data: Map<String, Any>?): Notification? {
        if (data == null) return null
        return Notification(
            id = id,
            title = data["title"] as? String ?: "",
            message = data["message"] as? String ?: "",
            type = try { NotificationType.valueOf(data["type"] as String) } catch (_: Exception) { NotificationType.SYSTEM_ALERT },
            priority = try { NotificationPriority.valueOf(data["priority"] as String) } catch (_: Exception) { NotificationPriority.LOW },
            isRead = data["isRead"] as? Boolean ?: false,
            recipientId = data["recipientId"] as? String,
            recipientRole = try { AdminRole.valueOf(data["recipientRole"] as String) } catch (_: Exception) { null },
            relatedEntityId = data["relatedEntityId"] as? String,
            relatedEntityType = data["relatedEntityType"] as? String,
            createdAt = (data["createdAt"] as? com.google.firebase.Timestamp)?.toDate() ?: Date(),
            updatedAt = (data["updatedAt"] as? com.google.firebase.Timestamp)?.toDate() ?: Date()
        )
    }

    private fun notificationToMap(n: Notification): Map<String, Any?> {
        return mapOf(
            "title" to n.title,
            "message" to n.message,
            "type" to n.type.name,
            "priority" to n.priority.name,
            "isRead" to n.isRead,
            "recipientId" to n.recipientId,
            "recipientRole" to if (n.recipientId != null) null else (n.recipientRole?.name ?: "GLOBAL"),
            "relatedEntityId" to n.relatedEntityId,
            "relatedEntityType" to n.relatedEntityType,
            "createdAt" to n.createdAt,
            "updatedAt" to n.updatedAt
        )
    }
}
