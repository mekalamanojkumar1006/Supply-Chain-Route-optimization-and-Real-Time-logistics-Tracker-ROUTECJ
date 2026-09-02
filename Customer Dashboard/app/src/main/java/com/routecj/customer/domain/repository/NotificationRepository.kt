package com.routecj.customer.domain.repository

import com.routecj.customer.domain.model.CustomerNotification
import kotlinx.coroutines.flow.Flow

interface NotificationRepository {

    /**
     * Real-time stream of notifications for [customerId], newest first.
     * Emits a new list whenever Firestore data changes.
     */
    fun getNotificationsFlow(customerId: String): Flow<Result<List<CustomerNotification>>>

    /**
     * Returns a real-time count of unread notifications for the given customer.
     */
    fun getUnreadCountFlow(customerId: String): Flow<Int>

    /**
     * Marks a single notification as read.
     * Only updates the document if it belongs to [customerId].
     */
    suspend fun markAsRead(notificationId: String, customerId: String): Result<Unit>

    /**
     * Batch-marks all unread notifications as read for [customerId].
     */
    suspend fun markAllAsRead(customerId: String): Result<Unit>

    /**
     * Saves a new notification record.
     * Used by client-side triggers (BOOKING_CREATED, PAYMENT_SUCCESS).
     * Deduplication: uses stable notificationId to prevent duplicate writes.
     */
    suspend fun saveNotification(notification: CustomerNotification): Result<Unit>
}
