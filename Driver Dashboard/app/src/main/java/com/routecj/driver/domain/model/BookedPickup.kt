package com.routecj.driver.domain.model

import java.util.Date

/**
 * Domain model representing a Customer Pickup Booking item for the Booked Slots view.
 */
data class BookedPickup(
    val id: String,
    val orderNumber: String,
    val customerName: String,
    val customerPhone: String = "",
    val pickupAddress: String,
    val pickupPincode: String = "",
    val deliveryAddress: String = "",
    val scheduledDate: Date? = null,
    val scheduledSlot: String = "",
    val status: String,
    val itemName: String = "Freight Parcel",
    val quantity: Int = 1,
    val weight: Double = 0.0,
    val isFragile: Boolean = false,
    val specialInstructions: String = "",
    val driverArrived: Boolean = false,
    val driverArrivedAt: Date? = null,
    val otpVerified: Boolean = false,
    val otpVerifiedAt: Date? = null,
    val createdAt: Date = Date(),
    val totalAmount: Double = 0.0
)
