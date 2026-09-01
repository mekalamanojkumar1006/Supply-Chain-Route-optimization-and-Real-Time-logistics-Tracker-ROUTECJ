package com.routecj.driver.data.mapper

import com.google.firebase.Timestamp
import com.routecj.driver.domain.model.FuelType
import com.routecj.driver.domain.model.Vehicle
import com.routecj.driver.domain.model.VehicleStatus
import com.routecj.driver.domain.model.VehicleType
import java.util.Date

/**
 * Mapper between Firestore document snapshots/maps and Vehicle domain models.
 */
object VehicleMapper {

    fun mapToDomain(id: String, data: Map<String, Any>?): Vehicle {
        val map = data ?: emptyMap()

        val vehicleNumber = map["vehicleNumber"] as? String ?: (map["registrationNumber"] as? String ?: "")
        val registrationNumber = map["registrationNumber"] as? String ?: (map["vehicleNumber"] as? String ?: "")

        val typeStr = (map["vehicleType"] as? String) ?: (map["type"] as? String ?: "VAN")
        val vehicleType = try {
            VehicleType.valueOf(typeStr.uppercase())
        } catch (_: Exception) {
            VehicleType.VAN
        }

        val statusStr = (map["status"] as? String) ?: "AVAILABLE"
        val status = try {
            VehicleStatus.valueOf(statusStr.uppercase())
        } catch (_: Exception) {
            VehicleStatus.AVAILABLE
        }

        val fuelStr = (map["fuelType"] as? String) ?: "DIESEL"
        val fuelType = try {
            FuelType.valueOf(fuelStr.uppercase())
        } catch (_: Exception) {
            FuelType.DIESEL
        }

        return Vehicle(
            id = id,
            vehicleNumber = vehicleNumber,
            vehicleType = vehicleType,
            brand = map["brand"] as? String ?: (map["make"] as? String ?: ""),
            model = map["model"] as? String ?: "",
            registrationNumber = registrationNumber,
            driverId = map["driverId"] as? String ?: (map["assignedDriverId"] as? String),
            driverName = map["driverName"] as? String ?: "",
            capacity = (map["capacity"] as? Number)?.toDouble() ?: 0.0,
            capacityUnit = map["capacityUnit"] as? String ?: "tons",
            imageUrl = map["imageUrl"] as? String,
            fuelLevel = (map["fuelLevel"] as? Number)?.toDouble() ?: 100.0,
            status = status,
            lastServiceDate = (map["lastServiceDate"] as? Timestamp)?.toDate()
                ?: (map["lastServiceDate"] as? Date) ?: Date(),
            nextServiceDate = (map["nextServiceDate"] as? Timestamp)?.toDate()
                ?: (map["nextServiceDate"] as? Date) ?: Date(),
            insuranceExpiry = (map["insuranceExpiry"] as? Timestamp)?.toDate()
                ?: (map["insuranceExpiry"] as? Date) ?: Date(),
            currentLatitude = (map["currentLatitude"] as? Number)?.toDouble() ?: 0.0,
            currentLongitude = (map["currentLongitude"] as? Number)?.toDouble() ?: 0.0,
            speed = (map["speed"] as? Number)?.toDouble() ?: 0.0,
            location = map["location"] as? String ?: "",
            odometer = (map["odometer"] as? Number)?.toDouble() ?: 0.0,
            createdAt = (map["createdAt"] as? Timestamp)?.toDate()
                ?: (map["createdAt"] as? Date) ?: Date(),
            updatedAt = (map["updatedAt"] as? Timestamp)?.toDate()
                ?: (map["updatedAt"] as? Date) ?: Date(),
            make = map["make"] as? String ?: "",
            year = (map["year"] as? Number)?.toInt() ?: 2024,
            assignedDriverId = map["assignedDriverId"] as? String ?: (map["driverId"] as? String),
            mileage = (map["mileage"] as? Number)?.toDouble() ?: 0.0,
            fuelType = fuelType
        )
    }
}
