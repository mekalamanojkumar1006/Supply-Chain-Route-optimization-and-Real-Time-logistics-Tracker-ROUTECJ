package com.routecj.driver.domain.model

import java.util.Date

/**
 * Domain model for Order.
 * Exactly matches the existing RouteCJ Firebase 'orders' collection schema.
 */
data class Order(
    val id: String = "",
    val orderNumber: String = "",
    val customerName: String = "",
    val customerPhone: String = "",
    val customerAddress: String = "",
    val pickupLocation: String = "",
    val deliveryLocation: String = "",
    val pickupAddress: String = "",
    val pickupPincode: String = "",
    val deliveryAddress: String = "",
    val deliveryPincode: String = "",
    val orderType: String = "",
    val weight: Double = 0.0,
    val quantity: Int = 0,
    val priority: String = "Medium",
    val paymentStatus: String = "Pending",
    val status: OrderStatus = OrderStatus.PENDING,
    val assignedDriverId: String? = null,
    val assignedVehicleId: String? = null,
    val estimatedDeliveryDate: Date? = null,
    val remarks: String = "",
    val createdAt: Date = Date(),
    val updatedAt: Date = Date(),

    // Legacy/Internal compatibility fields
    val customerId: String = "",
    val driverId: String? = null,
    val driverName: String? = null,
    val driverPhone: String? = null,
    val vehicleId: String? = null,
    val vehicleRegistration: String? = null,
    val vehicleType: String? = null,
    val godownId: String? = null,
    val origin: Location = Location(0.0, 0.0, "", "", "", ""),
    val destination: Location = Location(0.0, 0.0, "", "", "", ""),
    val items: List<OrderItem> = emptyList(),
    val totalAmount: Double = 0.0,
    val trackingId: String = "",
    val estimatedTime: String = "",
    val notes: String = "",

    // Role-specific workflow fields
    val otpVerified: Boolean = false,
    val otpVerifiedAt: Date? = null,
    val driverArrived: Boolean = false,
    val driverArrivedAt: Date? = null,
    val pickupSlot: String = "",
    val qrId: String? = null,
    val qrStatus: String? = null,
    val qrGeneratedBy: String? = null,
    val qrGeneratedAt: Date? = null,
    val verificationToken: String? = null,
    val parcelId: String? = null,

    // Godown parcel and item specifications
    val itemName: String = "",
    val itemDescription: String = "",
    val length: Double = 0.0,
    val width: Double = 0.0,
    val height: Double = 0.0,
    val isFragile: Boolean = false,
    val specialInstructions: String = "",

    // Audit and creator fields
    val createdBy: String = "",
    val createdByUid: String = "",
    val createdByRole: String = "",
    val source: String = "APP",

    // Delivery completion metadata
    val deliveredAt: Date? = null,
    val deliveredBy: String = "",
    val deliveredByUid: String = "",
    val deliveryOtp: String = "",
    val deliveryRemarks: String = ""
)

/**
 * Order Status enum.
 * Matches existing backend status values exactly.
 */
enum class OrderStatus {
    PENDING,
    ASSIGNED,
    PICKED_UP,
    PENDING_GODOWN_REVIEW,
    QR_GENERATED,
    READY_FOR_DISPATCH,
    DISPATCHED,
    IN_TRANSIT,
    DELIVERED,
    CANCELLED,
    FAILED
}

/**
 * Order Item model.
 */
data class OrderItem(
    val id: String = "",
    val orderId: String = "",
    val productId: String = "",
    val quantity: Double = 0.0,
    val unit: String = "",
    val price: Double = 0.0
)

/**
 * Location model for coordinates.
 */
data class Location(
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val address: String = "",
    val city: String = "",
    val state: String = "",
    val pincode: String = ""
)
