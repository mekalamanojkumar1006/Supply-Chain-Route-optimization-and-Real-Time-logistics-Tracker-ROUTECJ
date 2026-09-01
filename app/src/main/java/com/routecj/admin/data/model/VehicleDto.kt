package com.routecj.admin.data.model

import com.google.gson.annotations.SerializedName

/**
 * Data Transfer Object for Vehicle.
 * Used for network payloads and serializations.
 */
data class VehicleDto(
    @SerializedName("id")
    val id: String,

    @SerializedName("vehicle_number")
    val vehicleNumber: String,

    @SerializedName("vehicle_type")
    val vehicleType: String,

    @SerializedName("brand")
    val brand: String,

    @SerializedName("model")
    val model: String,

    @SerializedName("registration_number")
    val registrationNumber: String,

    @SerializedName("driver_id")
    val driverId: String? = null,

    @SerializedName("driver_name")
    val driverName: String,

    @SerializedName("capacity")
    val capacity: Double,

    @SerializedName("fuel_level")
    val fuelLevel: Double,

    @SerializedName("status")
    val status: String,

    @SerializedName("last_service_date")
    val lastServiceDate: String,

    @SerializedName("insurance_expiry")
    val insuranceExpiry: String,

    @SerializedName("current_latitude")
    val currentLatitude: Double,

    @SerializedName("current_longitude")
    val currentLongitude: Double,

    @SerializedName("speed")
    val speed: Double,

    @SerializedName("location")
    val location: String,

    @SerializedName("odometer")
    val odometer: Double,

    @SerializedName("created_at")
    val createdAt: String,

    @SerializedName("updated_at")
    val updatedAt: String
)
