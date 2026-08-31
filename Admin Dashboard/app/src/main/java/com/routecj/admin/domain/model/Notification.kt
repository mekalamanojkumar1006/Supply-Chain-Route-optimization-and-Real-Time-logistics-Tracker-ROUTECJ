package com.routecj.admin.domain.model

import java.util.Date

/**
 * Domain model representing a Notification or Alert.
 */
data class Notification(
    val id: String = "",
    val title: String = "",
    val message: String = "",
    val type: NotificationType = NotificationType.SYSTEM_ALERT,
    val priority: NotificationPriority = NotificationPriority.LOW,
    val isRead: Boolean = false,
    val recipientId: String? = null, // null means broadcast to role
    val recipientRole: AdminRole? = null, // null with null recipientId means global
    val relatedEntityId: String? = null,
    val relatedEntityType: String? = null, // "ORDER", "VEHICLE", "DRIVER", "GODOWN", "DISPATCH"
    val createdAt: Date = Date(),
    val updatedAt: Date = Date()
)

enum class NotificationType {
    ORDER_CREATED,
    ORDER_ASSIGNED,
    ORDER_STATUS_CHANGED,
    DRIVER_ASSIGNED,
    DRIVER_STATUS_CHANGED,
    VEHICLE_ASSIGNED,
    VEHICLE_STATUS_CHANGED,
    VEHICLE_SERVICE_DUE,
    DISPATCH_CREATED,
    TRIP_STARTED,
    TRIP_COMPLETED,
    TRIP_CANCELLED,
    GODOWN_CAPACITY_WARNING,
    GODOWN_CAPACITY_CRITICAL,
    SYSTEM_ALERT
}

enum class NotificationPriority {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
}
