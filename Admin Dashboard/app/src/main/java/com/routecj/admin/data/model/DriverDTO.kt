package com.routecj.admin.data.model

import com.google.gson.annotations.SerializedName

/**
 * Data Transfer Object for Driver.
 * Used for network payloads and serializations.
 */
data class DriverDTO(
    @SerializedName("id")
    val id: String,

    @SerializedName("uid")
    val uid: String? = null,

    @SerializedName("name")
    val name: String,

    @SerializedName("email")
    val email: String,

    @SerializedName("phone")
    val phone: String,

    @SerializedName("role")
    val role: String? = "DRIVER",

    @SerializedName("license_number")
    val licenseNumber: String,

    @SerializedName("license_expiry_date")
    val licenseExpiryDate: String,

    @SerializedName("status")
    val status: String,

    @SerializedName("assigned_vehicle")
    val assignedVehicle: String? = null,

    @SerializedName("assigned_vehicle_id")
    val assignedVehicleId: String? = null,

    @SerializedName("rating")
    val rating: Double,

    @SerializedName("total_deliveries")
    val totalDeliveries: Int,

    @SerializedName("completed_deliveries")
    val completedDeliveries: Int,

    @SerializedName("profile_image")
    val profileImage: String? = null,

    @SerializedName("current_latitude")
    val currentLatitude: Double,

    @SerializedName("current_longitude")
    val currentLongitude: Double,

    @SerializedName("address")
    val address: String,

    @SerializedName("is_active")
    val isActive: Boolean? = true,

    @SerializedName("joined_date")
    val joinedDate: String,

    @SerializedName("last_active")
    val lastActive: String,

    @SerializedName("created_at")
    val createdAt: String
)
