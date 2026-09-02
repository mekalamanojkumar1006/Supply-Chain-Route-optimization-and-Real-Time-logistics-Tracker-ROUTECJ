package com.routecj.admin.domain.model

import java.util.Date

/**
 * Domain model for Vehicle Log.
 * Represents an entry/exit log for a vehicle.
 */
data class VehicleLog(
    val id: String,
    val vehicleId: String,
    val vehicleNumber: String,
    val driverName: String,
    val timeIn: Date?,
    val timeOut: Date?,
    val gateNumber: String,
    val date: Date,
    val remarks: String? = null
)
