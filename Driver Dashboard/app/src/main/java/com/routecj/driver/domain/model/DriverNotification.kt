package com.routecj.driver.domain.model

import java.util.Date

/**
 * Domain model for a Driver Notification.
 * Maps to the RouteCJ 'notifications' collection.
 */
data class DriverNotification(
    val id: String = "",
    val recipientId: String = "",
    val recipientRole: String = "DRIVER",
    val title: String = "",
    val message: String = "",
    val type: NotificationType = NotificationType.SYSTEM,
    val orderId: String? = null,
    val dispatchId: String? = null,
    val tripId: String? = null,
    val isRead: Boolean = false,
    val createdAt: Date = Date()
)

/**
 * Notification types supported across RouteCJ Driver operations.
 */
enum class NotificationType {
    NEW_TRIP,
    TRIP_ASSIGNED,
    TRIP_CANCELLED,
    TRIP_UPDATED,
    PICKUP_REMINDER,
    CUSTOMER_ARRIVAL,
    DISPATCH,
    DELIVERY,
    DELIVERY_REMINDER,
    DRIVER_ARRIVAL,
    SYSTEM
}

enum class NotificationFilter {
    ALL,
    UNREAD
}
