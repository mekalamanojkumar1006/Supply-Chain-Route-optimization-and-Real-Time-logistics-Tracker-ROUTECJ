package com.routecj.customer.domain.model

/**
 * Customer notification domain model.
 *
 * SECURITY: This model must NEVER contain:
 *  - OTP values
 *  - Passwords or auth tokens
 *  - Payment credentials (card, CVV, UPI PIN)
 *  - Private driver personal data
 *  - Any sensitive information
 */
data class CustomerNotification(
    val notificationId: String = "",
    val customerId: String = "",
    val orderId: String? = null,
    val title: String = "",
    val message: String = "",
    val type: String = NotificationType.GENERAL.name,
    val createdAt: Long = System.currentTimeMillis(),
    val read: Boolean = false
) {
    val notificationType: NotificationType
        get() = NotificationType.fromString(type)
}
