package com.routecj.driver.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.routecj.driver.core.util.Result
import com.routecj.driver.data.mapper.NotificationMapper
import com.routecj.driver.domain.model.DriverNotification
import com.routecj.driver.domain.repository.NotificationRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.Date

/**
 * Firestore implementation of NotificationRepository.
 * Queries 'notifications' where recipientId == driverId or driverId == driverId.
 */
class FirestoreNotificationRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : NotificationRepository {

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

    override fun observeDriverNotifications(driverId: String): Flow<Result<List<DriverNotification>>> = callbackFlow {
        val listener = firestore.collection("notifications")
            .whereEqualTo("recipientId", driverId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Result.Error(error.message ?: "Failed to observe notifications", error))
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val notifications = snapshot.documents.mapNotNull { doc ->
                        try {
                            NotificationMapper.mapToDomain(doc.id, doc.data)
                        } catch (_: Exception) {
                            null
                        }
                    }.sortedByDescending { it.createdAt }

                    trySend(Result.Success(notifications))
                }
            }
        awaitClose { listener.remove() }
    }

    override fun observeUnreadCount(driverId: String): Flow<Int> = callbackFlow {
        val listener = firestore.collection("notifications")
            .whereEqualTo("recipientId", driverId)
            .whereEqualTo("isRead", false)
            .addSnapshotListener { snapshot, error ->
                if (error == null && snapshot != null) {
                    trySend(snapshot.size())
                } else {
                    trySend(0)
                }
            }
        awaitClose { listener.remove() }
    }

    override suspend fun markNotificationAsRead(notificationId: String): Result<Unit> = try {
        firestore.collection("notifications").document(notificationId)
            .update(
                mapOf(
                    "isRead" to true,
                    "readAt" to Date()
                )
            ).await()
        Result.Success(Unit)
    } catch (e: Exception) {
        Result.Error(e.message ?: "Failed to mark notification as read", e)
    }

    override suspend fun markAllNotificationsAsRead(driverId: String): Result<Unit> = try {
        requireAuth(driverId)
        val unreadDocs = firestore.collection("notifications")
            .whereEqualTo("recipientId", driverId)
            .whereEqualTo("isRead", false)
            .get()
            .await()

        if (!unreadDocs.isEmpty) {
            val batch = firestore.batch()
            for (doc in unreadDocs.documents) {
                batch.update(doc.reference, mapOf("isRead" to true, "readAt" to Date()))
            }
            batch.commit().await()
        }
        Result.Success(Unit)
    } catch (e: Exception) {
        Result.Error(e.message ?: "Failed to mark all as read", e)
    }

    override suspend fun updateFcmToken(driverId: String, token: String): Result<Unit> = try {
        requireAuth(driverId)
        firestore.collection("drivers").document(driverId)
            .update(
                mapOf(
                    "fcmToken" to token,
                    "notificationToken" to token,
                    "lastActive" to Date()
                )
            ).await()
        Result.Success(Unit)
    } catch (e: Exception) {
        Result.Error(e.message ?: "Failed to update FCM token", e)
    }
}
