package com.routecj.admin.domain.repository

import com.routecj.admin.core.util.Result
import com.routecj.admin.domain.model.AdminRole
import com.routecj.admin.domain.model.Notification
import kotlinx.coroutines.flow.Flow

interface NotificationRepository {
    suspend fun getNotifications(role: AdminRole, uid: String): Flow<Result<List<Notification>>>
    suspend fun markAsRead(notificationId: String): Result<Unit>
    suspend fun markAllAsRead(role: AdminRole, uid: String): Result<Unit>
    suspend fun deleteNotification(notificationId: String): Result<Unit>
    suspend fun createNotification(notification: Notification): Result<Unit>
}
