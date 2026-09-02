package com.routecj.driver.domain.repository

import com.routecj.driver.core.util.Result
import com.routecj.driver.domain.model.DriverNotification
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for Driver Notification operations.
 */
interface NotificationRepository {
    fun observeDriverNotifications(driverId: String): Flow<Result<List<DriverNotification>>>
    fun observeUnreadCount(driverId: String): Flow<Int>
    suspend fun markNotificationAsRead(notificationId: String): Result<Unit>
    suspend fun markAllNotificationsAsRead(driverId: String): Result<Unit>
    suspend fun updateFcmToken(driverId: String, token: String): Result<Unit>
}
