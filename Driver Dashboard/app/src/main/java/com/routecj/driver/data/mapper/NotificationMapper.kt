package com.routecj.driver.data.mapper

import com.google.firebase.Timestamp
import com.routecj.driver.domain.model.DriverNotification
import com.routecj.driver.domain.model.NotificationType
import java.util.Date

/**
 * Mapper between Firestore document snapshots/maps and DriverNotification domain models.
 */
object NotificationMapper {

    fun mapToDomain(id: String, data: Map<String, Any>?): DriverNotification {
        val map = data ?: emptyMap()

        val recipientId = map["recipientId"] as? String ?: (map["driverId"] as? String ?: (map["userId"] as? String ?: ""))
        val recipientRole = map["recipientRole"] as? String ?: "DRIVER"
        val title = map["title"] as? String ?: "RouteCJ Notification"
        val message = map["message"] as? String ?: (map["body"] as? String ?: "")

        val typeStr = (map["type"] as? String) ?: (map["nav_type"] as? String ?: "SYSTEM")
        val type = try {
            NotificationType.valueOf(typeStr.uppercase())
        } catch (_: Exception) {
            when (typeStr.uppercase()) {
                "ASSIGNED" -> NotificationType.TRIP_ASSIGNED
                "CANCELLED" -> NotificationType.TRIP_CANCELLED
                "UPDATED" -> NotificationType.TRIP_UPDATED
                "ARRIVAL" -> NotificationType.CUSTOMER_ARRIVAL
                "PICKUP" -> NotificationType.PICKUP_REMINDER
                else -> NotificationType.SYSTEM
            }
        }

        val orderId = map["orderId"] as? String
        val dispatchId = map["dispatchId"] as? String
        val tripId = map["tripId"] as? String ?: (map["nav_trip_id"] as? String ?: (dispatchId ?: orderId))
        val isRead = map["isRead"] as? Boolean ?: (map["read"] as? Boolean ?: false)

        val createdAt = (map["createdAt"] as? Timestamp)?.toDate()
            ?: (map["createdAt"] as? Date)
            ?: (map["timestamp"] as? Timestamp)?.toDate()
            ?: Date()

        return DriverNotification(
            id = id,
            recipientId = recipientId,
            recipientRole = recipientRole,
            title = title,
            message = message,
            type = type,
            orderId = orderId,
            dispatchId = dispatchId,
            tripId = tripId,
            isRead = isRead,
            createdAt = createdAt
        )
    }
}
