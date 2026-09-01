package com.routecj.admin.domain.model

import java.util.Date

/**
 * Domain model representing a Godown (Warehouse).
 */
data class Godown(
    val id: String = "",
    val name: String = "",
    val address: String = "",
    val city: String = "",
    val state: String = "",
    val pincode: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val capacity: Double = 0.0, // in tons
    val currentStock: Double = 0.0, // in tons
    val managerId: String? = null,
    val managerName: String? = null,
    val phone: String = "",
    val status: GodownStatus = GodownStatus.ACTIVE,
    val createdAt: Date = Date(),
    val updatedAt: Date = Date()
) {
    val availableCapacity: Double get() = (capacity - currentStock).coerceAtLeast(0.0)
    val occupancyPercentage: Int get() = if (capacity > 0) ((currentStock / capacity) * 100).toInt() else 0
}

enum class GodownStatus {
    ACTIVE,
    INACTIVE,
    MAINTENANCE
}
