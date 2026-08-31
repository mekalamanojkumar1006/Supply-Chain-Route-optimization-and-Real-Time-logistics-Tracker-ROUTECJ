package com.routecj.admin.domain.model

import java.util.Date

/**
 * Domain model for Vehicle.
 * Represents a vehicle in the business logic layer.
 */
data class Vehicle(
    val id: String = "",
    val vehicleNumber: String = "",
    val vehicleType: VehicleType = VehicleType.VAN,
    val brand: String = "",
    val model: String = "",
    val registrationNumber: String = "",
    val driverId: String? = null,
    val driverName: String = "",
    val capacity: Double = 0.0,
    val capacityUnit: String = "tons",
    val imageUrl: String? = null,
    val fuelLevel: Double = 100.0,
    val status: VehicleStatus = VehicleStatus.AVAILABLE,
    val lastServiceDate: Date = Date(),
    val nextServiceDate: Date = Date(),
    val insuranceExpiry: Date = Date(),
    val currentLatitude: Double = 0.0,
    val currentLongitude: Double = 0.0,
    val speed: Double = 0.0,
    val location: String = "",
    val odometer: Double = 0.0,
    val createdAt: Date = Date(),
    val updatedAt: Date = Date(),
    // Keep original parameters as defaults for backward compatibility
    val make: String = "",
    val year: Int = 2024,
    val assignedDriverId: String? = null,
    val mileage: Double = 0.0,
    val fuelType: FuelType = FuelType.DIESEL
)

/**
 * Vehicle Type enum.
 */
enum class VehicleType {
    TWO_WHEELER,
    AUTO_RICKSHAW,
    TRUCK_3T,
    TRUCK_5T,
    TRUCK_10T,
    TRUCK_20T,
    TEMPO,
    VAN
}

/**
 * Vehicle Status enum.
 */
enum class VehicleStatus {
    AVAILABLE,
    ASSIGNED,
    IN_TRANSIT,
    MAINTENANCE,
    INACTIVE
}

/**
 * Fuel Type enum.
 */
enum class FuelType {
    PETROL,
    DIESEL,
    CNG,
    ELECTRIC
}

