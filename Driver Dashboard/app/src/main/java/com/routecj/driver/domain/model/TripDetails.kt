package com.routecj.driver.domain.model

import java.util.Date

/**
 * Rich domain model for a Trip Details view combining Order and Dispatch attributes.
 */
data class TripDetails(
    val tripId: String,
    val orderId: String,
    val orderNumber: String,
    val customerName: String,
    val customerPhone: String = "",
    val pickupAddress: String,
    val deliveryAddress: String,
    val status: String,
    val priority: String = "Medium",
    val vehicleId: String? = null,
    val vehicleRegistration: String? = null,
    val vehicleType: String? = null,
    val itemName: String = "Freight Parcel",
    val weight: Double = 0.0,
    val quantity: Int = 1,
    val isFragile: Boolean = false,
    val specialInstructions: String = "",
    val totalAmount: Double = 0.0,
    val driverId: String,
    val driverName: String? = null,
    val scheduledDate: Date? = null,
    val originLat: Double = 0.0,
    val originLng: Double = 0.0,
    val destinationLat: Double = 0.0,
    val destinationLng: Double = 0.0,
    val createdAt: Date = Date(),
    val isDispatchRecord: Boolean = true,
    val otpVerified: Boolean = false,
    val driverArrived: Boolean = false
)
