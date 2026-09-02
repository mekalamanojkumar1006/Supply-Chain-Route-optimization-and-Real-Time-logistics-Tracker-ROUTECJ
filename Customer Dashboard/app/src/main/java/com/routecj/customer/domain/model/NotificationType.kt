package com.routecj.customer.domain.model

/**
 * All customer-facing notification event types.
 * Server-side producers (Admin, Driver, Godown) write these as string values into Firestore.
 * Customer App reads them to determine the icon and display message.
 */
enum class NotificationType {
    BOOKING_CREATED,
    DRIVER_ASSIGNED,
    DRIVER_ARRIVED,
    OTP_VERIFIED,
    PARCEL_SUBMITTED,
    GODOWN_REVIEW,
    GODOWN_APPROVED,
    READY_FOR_DISPATCH,
    DISPATCHED,
    IN_TRANSIT,
    DELIVERED,
    PAYMENT_SUCCESS,
    PAYMENT_FAILED,
    GENERAL;

    companion object {
        fun fromString(value: String?): NotificationType =
            entries.firstOrNull { it.name == value } ?: GENERAL
    }
}
