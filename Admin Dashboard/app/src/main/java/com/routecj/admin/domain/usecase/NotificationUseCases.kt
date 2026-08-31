package com.routecj.admin.domain.usecase

import com.routecj.admin.core.util.Result
import com.routecj.admin.domain.model.AdminRole
import com.routecj.admin.domain.model.Notification
import com.routecj.admin.domain.repository.NotificationRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetNotificationsUseCase @Inject constructor(
    private val repository: NotificationRepository
) {
    suspend operator fun invoke(role: AdminRole, uid: String): Flow<Result<List<Notification>>> = 
        repository.getNotifications(role, uid)
}

class CreateNotificationUseCase @Inject constructor(
    private val repository: NotificationRepository
) {
    suspend operator fun invoke(notification: Notification): Result<Unit> = 
        repository.createNotification(notification)
}

class MarkNotificationReadUseCase @Inject constructor(
    private val repository: NotificationRepository
) {
    suspend operator fun invoke(id: String): Result<Unit> = 
        repository.markAsRead(id)
}

class MarkAllNotificationsReadUseCase @Inject constructor(
    private val repository: NotificationRepository
) {
    suspend operator fun invoke(role: AdminRole, uid: String): Result<Unit> = 
        repository.markAllAsRead(role, uid)
}

class DeleteNotificationUseCase @Inject constructor(
    private val repository: NotificationRepository
) {
    suspend operator fun invoke(id: String): Result<Unit> = 
        repository.deleteNotification(id)
}
