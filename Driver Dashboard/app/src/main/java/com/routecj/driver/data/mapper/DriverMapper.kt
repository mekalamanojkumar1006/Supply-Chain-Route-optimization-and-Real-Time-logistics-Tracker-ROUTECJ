package com.routecj.driver.data.mapper

import com.google.firebase.Timestamp
import com.routecj.driver.domain.model.Driver
import com.routecj.driver.domain.model.DriverStatus
import java.util.Date

/**
 * Mapper between Firestore document snapshots/maps and Driver domain models.
 */
object DriverMapper {

    fun mapToDomain(id: String, data: Map<String, Any>?): Driver {
        val map = data ?: emptyMap()

        val name = map["name"] as? String ?: ""
        val email = map["email"] as? String ?: ""
        val phone = map["phone"] as? String ?: ""
        val licenseNumber = map["licenseNumber"] as? String ?: ""

        val licenseExpiryDate = (map["licenseExpiryDate"] as? Timestamp)?.toDate()
            ?: (map["licenseExpiryDate"] as? Date) ?: Date()

        val statusStr = (map["status"] as? String) ?: "AVAILABLE"
        val status = try {
            DriverStatus.valueOf(statusStr.uppercase())
        } catch (_: Exception) {
            DriverStatus.AVAILABLE
        }

        val assignedVehicle = map["assignedVehicle"] as? String
        val assignedVehicleId = map["assignedVehicleId"] as? String
        val rating = (map["rating"] as? Number)?.toDouble() ?: 5.0
        val totalDeliveries = (map["totalDeliveries"] as? Number)?.toInt() ?: 0
        val completedDeliveries = (map["completedDeliveries"] as? Number)?.toInt() ?: 0
        val profileImage = map["profileImage"] as? String

        val currentLatitude = (map["currentLatitude"] as? Number)?.toDouble() ?: 0.0
        val currentLongitude = (map["currentLongitude"] as? Number)?.toDouble() ?: 0.0
        val address = map["address"] as? String ?: ""

        val joinedDate = (map["joinedDate"] as? Timestamp)?.toDate()
            ?: (map["joinedDate"] as? Date) ?: Date()
        val lastActive = (map["lastActive"] as? Timestamp)?.toDate()
            ?: (map["lastActive"] as? Date) ?: Date()
        val createdAt = (map["createdAt"] as? Timestamp)?.toDate()
            ?: (map["createdAt"] as? Date) ?: Date()

        return Driver(
            id = id,
            name = name,
            email = email,
            phone = phone,
            licenseNumber = licenseNumber,
            licenseExpiryDate = licenseExpiryDate,
            status = status,
            assignedVehicle = assignedVehicle,
            assignedVehicleId = assignedVehicleId,
            rating = rating,
            totalDeliveries = totalDeliveries,
            completedDeliveries = completedDeliveries,
            profileImage = profileImage,
            currentLatitude = currentLatitude,
            currentLongitude = currentLongitude,
            address = address,
            joinedDate = joinedDate,
            lastActive = lastActive,
            createdAt = createdAt
        )
    }

    fun mapToFirestore(driver: Driver): Map<String, Any> {
        return mapOf(
            "name" to driver.name,
            "email" to driver.email,
            "phone" to driver.phone,
            "licenseNumber" to driver.licenseNumber,
            "licenseExpiryDate" to driver.licenseExpiryDate,
            "status" to driver.status.name.lowercase(),
            "assignedVehicle" to (driver.assignedVehicle ?: ""),
            "assignedVehicleId" to (driver.assignedVehicleId ?: ""),
            "rating" to driver.rating,
            "totalDeliveries" to driver.totalDeliveries,
            "completedDeliveries" to driver.completedDeliveries,
            "profileImage" to (driver.profileImage ?: ""),
            "currentLatitude" to driver.currentLatitude,
            "currentLongitude" to driver.currentLongitude,
            "address" to driver.address,
            "joinedDate" to driver.joinedDate,
            "lastActive" to driver.lastActive,
            "createdAt" to driver.createdAt
        )
    }
}
