package com.routecj.driver.data.mapper

import com.google.firebase.Timestamp
import com.routecj.driver.domain.model.Dispatch
import com.routecj.driver.domain.model.DispatchStatus
import java.util.Date

/**
 * Mapper between Firestore document snapshots/maps and Dispatch domain models.
 */
object DispatchMapper {

    fun mapToDomain(id: String, data: Map<String, Any>?): Dispatch {
        val map = data ?: emptyMap()

        val rawStatus = (map["status"] as? String)?.uppercase() ?: "PENDING"
        val status = try {
            DispatchStatus.valueOf(rawStatus)
        } catch (_: Exception) {
            DispatchStatus.PENDING
        }

        return Dispatch(
            id = id,
            orderId = map["orderId"] as? String ?: "",
            orderNumber = map["orderNumber"] as? String ?: "",
            customerName = map["customerName"] as? String ?: "",
            pickupLocation = map["pickupLocation"] as? String ?: "",
            deliveryLocation = map["deliveryLocation"] as? String ?: "",
            driverId = map["driverId"] as? String,
            driverName = map["driverName"] as? String,
            vehicleId = map["vehicleId"] as? String,
            vehicleRegistration = map["vehicleRegistration"] as? String,
            status = status,
            priority = map["priority"] as? String ?: "Medium",
            estimatedDelivery = (map["estimatedDelivery"] as? Timestamp)?.toDate()
                ?: (map["estimatedDelivery"] as? Date),
            createdAt = (map["createdAt"] as? Timestamp)?.toDate()
                ?: (map["createdAt"] as? Date) ?: Date(),
            updatedAt = (map["updatedAt"] as? Timestamp)?.toDate()
                ?: (map["updatedAt"] as? Date) ?: Date(),
            remarks = map["remarks"] as? String
        )
    }
}
