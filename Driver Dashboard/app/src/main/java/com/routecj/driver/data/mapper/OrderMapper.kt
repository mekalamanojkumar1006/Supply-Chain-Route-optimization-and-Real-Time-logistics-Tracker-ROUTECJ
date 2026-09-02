package com.routecj.driver.data.mapper

import com.google.firebase.Timestamp
import com.routecj.driver.domain.model.Location
import com.routecj.driver.domain.model.Order
import com.routecj.driver.domain.model.OrderItem
import com.routecj.driver.domain.model.OrderStatus
import java.util.Date

/**
 * Mapper between Firestore document snapshots/maps and Order domain models.
 */
object OrderMapper {

    @Suppress("UNCHECKED_CAST")
    fun mapToDomain(id: String, data: Map<String, Any>?): Order {
        val map = data ?: emptyMap()

        val rawStatus = (map["status"] as? String)?.uppercase() ?: "PENDING"
        val status = try {
            OrderStatus.valueOf(rawStatus)
        } catch (_: Exception) {
            OrderStatus.PENDING
        }

        val rawItems = (map["items"] as? List<Map<String, Any>>) ?: emptyList()
        val items = rawItems.map { itemMap ->
            OrderItem(
                id = itemMap["id"] as? String ?: "",
                orderId = itemMap["orderId"] as? String ?: id,
                productId = itemMap["productId"] as? String ?: "",
                quantity = (itemMap["quantity"] as? Number)?.toDouble() ?: 0.0,
                unit = itemMap["unit"] as? String ?: "",
                price = (itemMap["price"] as? Number)?.toDouble() ?: 0.0
            )
        }

        val originMap = map["origin"] as? Map<String, Any>
        val origin = if (originMap != null) {
            Location(
                latitude = (originMap["latitude"] as? Number)?.toDouble() ?: 0.0,
                longitude = (originMap["longitude"] as? Number)?.toDouble() ?: 0.0,
                address = originMap["address"] as? String ?: "",
                city = originMap["city"] as? String ?: "",
                state = originMap["state"] as? String ?: "",
                pincode = originMap["pincode"] as? String ?: ""
            )
        } else {
            Location(0.0, 0.0, "", "", "", "")
        }

        val destMap = map["destination"] as? Map<String, Any>
        val destination = if (destMap != null) {
            Location(
                latitude = (destMap["latitude"] as? Number)?.toDouble() ?: 0.0,
                longitude = (destMap["longitude"] as? Number)?.toDouble() ?: 0.0,
                address = destMap["address"] as? String ?: "",
                city = destMap["city"] as? String ?: "",
                state = destMap["state"] as? String ?: "",
                pincode = destMap["pincode"] as? String ?: ""
            )
        } else {
            Location(0.0, 0.0, "", "", "", "")
        }

        return Order(
            id = id,
            orderNumber = map["orderNumber"] as? String ?: "",
            customerName = map["customerName"] as? String ?: "",
            customerPhone = map["customerPhone"] as? String ?: "",
            customerAddress = map["customerAddress"] as? String ?: "",
            pickupLocation = map["pickupLocation"] as? String ?: (map["pickupAddress"] as? String ?: ""),
            deliveryLocation = map["deliveryLocation"] as? String ?: (map["deliveryAddress"] as? String ?: ""),
            pickupAddress = map["pickupAddress"] as? String ?: (map["pickupLocation"] as? String ?: ""),
            pickupPincode = map["pickupPincode"] as? String ?: "",
            deliveryAddress = map["deliveryAddress"] as? String ?: (map["deliveryLocation"] as? String ?: (map["customerAddress"] as? String ?: "")),
            deliveryPincode = map["deliveryPincode"] as? String ?: "",
            orderType = map["orderType"] as? String ?: "",
            weight = (map["weight"] as? Number)?.toDouble() ?: 0.0,
            quantity = (map["quantity"] as? Number)?.toInt() ?: 0,
            priority = map["priority"] as? String ?: "Medium",
            paymentStatus = map["paymentStatus"] as? String ?: "Pending",
            status = status,
            assignedDriverId = map["assignedDriverId"] as? String ?: (map["driverId"] as? String),
            assignedVehicleId = map["assignedVehicleId"] as? String ?: (map["vehicleId"] as? String),
            estimatedDeliveryDate = (map["estimatedDeliveryDate"] as? Timestamp)?.toDate()
                ?: (map["estimatedDeliveryDate"] as? Date),
            remarks = map["remarks"] as? String ?: "",
            createdAt = (map["createdAt"] as? Timestamp)?.toDate()
                ?: (map["createdAt"] as? Date) ?: Date(),
            updatedAt = (map["updatedAt"] as? Timestamp)?.toDate()
                ?: (map["updatedAt"] as? Date) ?: Date(),

            customerId = map["customerId"] as? String ?: "",
            driverId = map["driverId"] as? String ?: (map["assignedDriverId"] as? String),
            driverName = map["driverName"] as? String,
            driverPhone = map["driverPhone"] as? String,
            vehicleId = map["vehicleId"] as? String ?: (map["assignedVehicleId"] as? String),
            vehicleRegistration = map["vehicleRegistration"] as? String,
            vehicleType = map["vehicleType"] as? String,
            godownId = map["godownId"] as? String,
            origin = origin,
            destination = destination,
            items = items,
            totalAmount = (map["totalAmount"] as? Number)?.toDouble() ?: 0.0,
            trackingId = map["trackingId"] as? String ?: "",
            estimatedTime = map["estimatedTime"] as? String ?: "",
            notes = map["notes"] as? String ?: "",

            otpVerified = map["otpVerified"] as? Boolean ?: false,
            otpVerifiedAt = (map["otpVerifiedAt"] as? Timestamp)?.toDate() ?: (map["otpVerifiedAt"] as? Date),
            driverArrived = map["driverArrived"] as? Boolean ?: false,
            driverArrivedAt = (map["driverArrivedAt"] as? Timestamp)?.toDate() ?: (map["driverArrivedAt"] as? Date),
            pickupSlot = map["pickupSlot"] as? String ?: (map["pickupTime"] as? String ?: (map["scheduledTime"] as? String ?: "")),
            qrId = map["qrId"] as? String,
            qrStatus = map["qrStatus"] as? String,
            qrGeneratedBy = map["qrGeneratedBy"] as? String,
            qrGeneratedAt = (map["qrGeneratedAt"] as? Timestamp)?.toDate() ?: (map["qrGeneratedAt"] as? Date),
            verificationToken = map["verificationToken"] as? String,
            parcelId = map["parcelId"] as? String,

            itemName = map["itemName"] as? String ?: "",
            itemDescription = map["itemDescription"] as? String ?: "",
            length = (map["length"] as? Number)?.toDouble() ?: 0.0,
            width = (map["width"] as? Number)?.toDouble() ?: 0.0,
            height = (map["height"] as? Number)?.toDouble() ?: 0.0,
            isFragile = map["isFragile"] as? Boolean ?: false,
            specialInstructions = map["specialInstructions"] as? String ?: "",

            createdBy = map["createdBy"] as? String ?: "",
            createdByUid = map["createdByUid"] as? String ?: "",
            createdByRole = map["createdByRole"] as? String ?: "",
            source = map["source"] as? String ?: "APP",

            deliveredAt = (map["deliveredAt"] as? Timestamp)?.toDate() ?: (map["deliveredAt"] as? Date),
            deliveredBy = map["deliveredBy"] as? String ?: "",
            deliveredByUid = map["deliveredByUid"] as? String ?: "",
            deliveryOtp = map["deliveryOtp"] as? String ?: "",
            deliveryRemarks = map["deliveryRemarks"] as? String ?: ""
        )
    }
}
